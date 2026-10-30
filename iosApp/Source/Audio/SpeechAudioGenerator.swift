import AVFoundation
import Foundation
import OSLog

@MainActor
final class SpeechAudioGenerator: SpeechAudioGenerating {
    private static let maximumBytes: Int64 = 256 * 1_024 * 1_024
    private static let timeout: Duration = .seconds(300)
    private static let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "FeedFlow", category: "SpeechAudio")

    private let synthesizerFactory: @MainActor () -> SpeechSynthesizerDriving
    private let writerFactory: @MainActor (URL, AVAudioFormat) throws -> SpeechAudioWriting
    private let voiceAvailable: () async -> Bool
    private let languageDetector: @Sendable (String) async -> String?
    private var generationID = UUID()
    private var cleanedTemporaryFiles = false
    private var activeSynthesizer: SpeechSynthesizerDriving?
    private var activeGenerationID: UUID?

    init(
        synthesizerFactory: @escaping @MainActor () -> SpeechSynthesizerDriving = { NativeSpeechSynthesizer() },
        writerFactory: @escaping @MainActor (URL, AVAudioFormat) throws -> SpeechAudioWriting = { url, format in
            try NativeSpeechAudioWriter(url: url, format: format)
        },
        voiceAvailable: @escaping () async -> Bool = { true },
        languageDetector: @escaping @Sendable (String) async -> String? = { await SpeechLanguageDetector.detect($0) }
    ) {
        self.synthesizerFactory = synthesizerFactory
        self.writerFactory = writerFactory
        self.voiceAvailable = voiceAvailable
        self.languageDetector = languageDetector
    }

    func generate(segments: [String], languageTag: String? = nil) async throws -> URL {
        generationID = UUID()
        let requestID = generationID
        let startedAt = ContinuousClock.now
        if !cleanedTemporaryFiles {
            let previousDirectories = try FileManager.default.contentsOfDirectory(
                at: FileManager.default.temporaryDirectory, includingPropertiesForKeys: nil
            )
            for previous in previousDirectories where previous.lastPathComponent.hasPrefix("reader-speech-") {
                try FileManager.default.removeItem(at: previous)
            }
            cleanedTemporaryFiles = true
        }
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("reader-speech-\(UUID().uuidString)", isDirectory: true)
        let outputURL = directory.appendingPathComponent("speech.caf")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        var writer: SpeechAudioWriting?
        var bytes: Int64 = 0
        var completedSegments = 0
        var requestSynthesizer: SpeechSynthesizerDriving?

        do {
            try await withTimeout {
                guard await self.voiceAvailable() else {
                    throw SpeechAudioGenerationError.voiceUnavailable
                }
                try Task.checkCancellation()
                guard self.generationID == requestID else { throw CancellationError() }
                self.activeSynthesizer?.stop()
                let synthesizer = self.synthesizerFactory()
                requestSynthesizer = synthesizer
                self.activeSynthesizer = synthesizer
                self.activeGenerationID = requestID
                let language: String?
                if let languageTag {
                    language = languageTag
                } else {
                    language = await self.languageDetector(Self.languageSample(from: segments))
                }
                try await synthesizer.prepare(languageTag: language)
                for segment in segments where !segment.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    try Task.checkCancellation()
                    guard self.generationID == requestID else { throw CancellationError() }
                    let utteranceFinished = try await synthesizer.synthesize(segment) { buffer in
                        guard self.generationID == requestID else { return }
                        guard buffer.frameLength > 0 else { return }
                        if writer == nil {
                            writer = try self.writerFactory(outputURL, buffer.format)
                        }
                        let frameBytes = Int64(buffer.frameLength) * Int64(buffer.format.streamDescription.pointee.mBytesPerFrame)
                        guard bytes <= Self.maximumBytes - frameBytes else {
                            throw SpeechAudioGenerationError.outputTooLarge
                        }
                        try writer?.append(buffer)
                        bytes += frameBytes
                    }
                    guard utteranceFinished else { throw SpeechAudioGenerationError.synthesisFailed }
                    completedSegments += 1
                }
            }
            try Task.checkCancellation()
            guard generationID == requestID else { throw CancellationError() }
            guard bytes > 0, let writer else { throw SpeechAudioGenerationError.emptyAudio }
            try writer.finish()
            if activeGenerationID == requestID {
                activeSynthesizer = nil
                activeGenerationID = nil
            }
            let elapsed = ContinuousClock.now - startedAt
            let elapsedMs = elapsed.components.seconds * 1_000 + elapsed.components.attoseconds / 1_000_000_000_000_000
            Self.logger.info("Speech generation: elapsedMs=\(elapsedMs, privacy: .public)")
            Self.logger.info("bytes=\(bytes, privacy: .public); segments=\(completedSegments, privacy: .public)")
            return outputURL
        } catch {
            if generationID == requestID {
                generationID = UUID()
            }
            requestSynthesizer?.stop()
            if activeGenerationID == requestID {
                activeSynthesizer = nil
                activeGenerationID = nil
            }
            try? FileManager.default.removeItem(at: directory)
            throw error
        }
    }

    private nonisolated static func languageSample(from segments: [String]) -> String {
        var sample = ""
        for segment in segments {
            let candidate = sample.isEmpty ? segment : " \(segment)"
            let remaining = 4_000 - sample.count
            guard remaining > 0 else { break }
            sample += String(candidate.prefix(remaining))
        }
        return sample
    }

    /// Voice discovery can synchronously wait on system services; keep it off the main actor.
    private func withTimeout<T: Sendable>(_ operation: @escaping @MainActor () async throws -> T) async throws -> T {
        try await withThrowingTaskGroup(of: T.self) { group in
            group.addTask { try await operation() }
            group.addTask {
                try await Task.sleep(for: Self.timeout)
                throw SpeechAudioGenerationError.timedOut
            }
            guard let result = try await group.next() else { throw SpeechAudioGenerationError.synthesisFailed }
            group.cancelAll()
            return result
        }
    }
}
