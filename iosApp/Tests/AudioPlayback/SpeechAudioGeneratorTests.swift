import AVFoundation
import XCTest

@MainActor
final class SpeechAudioGeneratorTests: XCTestCase {
    func testSegmentsAreSynthesizedInOrderIntoPlayableAudioFile() async throws {
        let synth = TestSpeechSynthesizer()
        var testWriter: TestSpeechAudioWriter?
        let generator = SpeechAudioGenerator(
            synthesizerFactory: { synth },
            writerFactory: { url, format in
                let writer = try TestSpeechAudioWriter(url: url, format: format)
                testWriter = writer
                synth.lastOutputPath = url.path
                return writer
            },
            voiceAvailable: { true }
        )

        let generation = Task { try await generator.generate(segments: ["first", "second"]) }
        try await synth.waitForCallCount(1)
        synth.finishCurrent(with: try Self.buffer(frames: 8_000))
        try await synth.waitForCallCount(2)
        synth.finishCurrent(with: try Self.buffer(frames: 8_000))
        let url = try await generation.value

        XCTAssertEqual(synth.texts, ["first", "second"])
        XCTAssertEqual(testWriter?.appendCount, 2)
        let file = try AVAudioFile(forReading: url)
        XCTAssertEqual(Double(file.length) / file.processingFormat.sampleRate, 1, accuracy: 0.02)
        try FileManager.default.removeItem(at: url.deletingLastPathComponent())
    }

    func testCancellationStopsSynthesisAndRemovesPartialOutput() async throws {
        let synth = TestSpeechSynthesizer()
        let generator = SpeechAudioGenerator(
            synthesizerFactory: { synth },
            writerFactory: { url, format in
                synth.lastOutputPath = url.path
                return try TestSpeechAudioWriter(url: url, format: format)
            },
            voiceAvailable: { true }
        )
        let task = Task { try await generator.generate(segments: ["one", "two"]) }
        try await synth.waitForCallCount(1)
        synth.send(try Self.buffer(frames: 32))
        task.cancel()
        do {
            _ = try await task.value
            XCTFail("Expected cancellation")
        } catch is CancellationError {
            XCTAssertTrue(synth.stopped)
        }
        XCTAssertFalse(FileManager.default.fileExists(atPath: synth.lastOutputPath ?? ""))
    }

    func testLateCallbackAfterCancellationIsRejected() async throws {
        let synth = TestSpeechSynthesizer()
        var testWriter: TestSpeechAudioWriter?
        let generator = SpeechAudioGenerator(
            synthesizerFactory: { synth },
            writerFactory: { url, format in
                let writer = try TestSpeechAudioWriter(url: url, format: format)
                testWriter = writer
                synth.lastOutputPath = url.path
                return writer
            },
            voiceAvailable: { true }
        )
        let task = Task { try await generator.generate(segments: ["one"]) }
        try await synth.waitForCallCount(1)
        synth.send(try Self.buffer(frames: 16))
        let appendCountBeforeCancellation = testWriter?.appendCount
        task.cancel()
        _ = try? await task.value
        synth.sendLate(try Self.buffer(frames: 16))
        await Task.yield()
        XCTAssertEqual(testWriter?.appendCount, appendCountBeforeCancellation)
        XCTAssertFalse(FileManager.default.fileExists(atPath: synth.lastOutputPath ?? ""))
    }

    func testMissingVoiceFailsBeforeSynthesis() async {
        let synth = TestSpeechSynthesizer()
        let generator = SpeechAudioGenerator(synthesizerFactory: { synth }, voiceAvailable: { false })
        do {
            _ = try await generator.generate(segments: ["hello"])
            XCTFail("Expected voice failure")
        } catch SpeechAudioGenerationError.voiceUnavailable {
            XCTAssertTrue(synth.texts.isEmpty)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testDefaultAvailabilityUsesTheSynthesizersImplicitVoice() async throws {
        let synth = TestSpeechSynthesizer()
        let generator = SpeechAudioGenerator(synthesizerFactory: { synth })
        let task = Task { try await generator.generate(segments: ["hello"]) }
        try await synth.waitForCallCount(1)
        synth.finishCurrent(with: try Self.buffer(frames: 8_000))
        let url = try await task.value

        XCTAssertEqual(synth.texts, ["hello"])
        try FileManager.default.removeItem(at: url.deletingLastPathComponent())
    }

    func testCancellationDuringVoiceAvailabilityCreatesNoSynthesizerOrAudioOutput() async throws {
        let gate = AsyncVoiceAvailabilityGate()
        let directoriesBefore = speechTemporaryDirectories()
        let synth = TestSpeechSynthesizer()
        var synthesizerFactoryCalls = 0
        var writerFactoryCalls = 0
        let generator = SpeechAudioGenerator(
            synthesizerFactory: {
                synthesizerFactoryCalls += 1
                return synth
            },
            writerFactory: { _, _ in
                writerFactoryCalls += 1
                throw SpeechAudioGenerationError.synthesisFailed
            },
            voiceAvailable: { await gate.wait() }
        )
        defer { gate.resume(true) }

        let task = Task { try await generator.generate(segments: ["cancelled"]) }
        try await waitUntilVoiceAvailabilityIsWaiting(gate)
        task.cancel()
        gate.resume(true)
        do {
            _ = try await task.value
            XCTFail("Expected cancellation")
        } catch is CancellationError {
            XCTAssertEqual(synthesizerFactoryCalls, 0)
            XCTAssertEqual(writerFactoryCalls, 0)
            XCTAssertTrue(synth.texts.isEmpty)
        }

        XCTAssertEqual(speechTemporaryDirectories(), directoriesBefore)
    }

    func testLateVoiceAvailabilityFromOldGenerationCannotStopNewSynthesizer() async throws {
        let availability = FirstVoiceCallGate()
        let synth = TestSpeechSynthesizer()
        let generator = SpeechAudioGenerator(
            synthesizerFactory: { synth },
            voiceAvailable: { await availability.check() }
        )
        defer { availability.resumeFirstCall(true) }

        let oldTask = Task { try await generator.generate(segments: ["old generation"]) }
        try await waitUntilVoiceAvailabilityIsWaiting(availability.gate)

        let newTask = Task { try await generator.generate(segments: ["new generation"]) }
        try await synth.waitForCallCount(1)
        availability.resumeFirstCall(true)

        do {
            _ = try await oldTask.value
            XCTFail("Expected the superseded generation to be cancelled")
        } catch is CancellationError {
            XCTAssertEqual(synth.texts, ["new generation"])
            XCTAssertFalse(synth.stopped)
        }

        synth.finishCurrent(with: try Self.buffer(frames: 8_000))
        let outputURL = try await newTask.value
        XCTAssertEqual(synth.texts, ["new generation"])
        XCTAssertFalse(synth.stopped)
        try FileManager.default.removeItem(at: outputURL.deletingLastPathComponent())
    }

    private func waitUntilVoiceAvailabilityIsWaiting(_ gate: AsyncVoiceAvailabilityGate) async throws {
        for _ in 0 ..< 10_000 {
            if gate.isWaiting { return }
            await Task.yield()
        }
        throw SpeechAudioGenerationError.timedOut
    }

    private func speechTemporaryDirectories() -> Set<String> {
        let names = (try? FileManager.default.contentsOfDirectory(atPath: NSTemporaryDirectory())) ?? []
        return Set(names.filter { $0.hasPrefix("reader-speech-") })
    }

    func testStartupCleanupRemovesOwnedOrphansAndPreservesCachedOutputOnLaterGeneration() async throws {
        let temporary = FileManager.default.temporaryDirectory
        let orphan = temporary.appendingPathComponent("reader-speech-\(UUID().uuidString)", isDirectory: true)
        let unrelated = temporary.appendingPathComponent("unrelated-speech-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: orphan, withIntermediateDirectories: true)
        try FileManager.default.createDirectory(at: unrelated, withIntermediateDirectories: true)
        try Data("old output".utf8).write(to: orphan.appendingPathComponent("speech.caf"))
        defer {
            try? FileManager.default.removeItem(at: orphan)
            try? FileManager.default.removeItem(at: unrelated)
        }
        let synth = TestSpeechSynthesizer()
        let generator = SpeechAudioGenerator(synthesizerFactory: { synth }, voiceAvailable: { true })
        let first = Task { try await generator.generate(segments: ["first"]) }
        try await synth.waitForCallCount(1)
        XCTAssertFalse(FileManager.default.fileExists(atPath: orphan.path))
        XCTAssertTrue(FileManager.default.fileExists(atPath: unrelated.path))
        synth.finishCurrent(with: try Self.buffer(frames: 8_000))
        let firstURL = try await first.value
        defer { try? FileManager.default.removeItem(at: firstURL.deletingLastPathComponent()) }

        let second = Task { try await generator.generate(segments: ["second"]) }
        try await synth.waitForCallCount(2)
        XCTAssertTrue(FileManager.default.fileExists(atPath: firstURL.path))
        synth.finishCurrent(with: try Self.buffer(frames: 8_000))
        let secondURL = try await second.value
        defer { try? FileManager.default.removeItem(at: secondURL.deletingLastPathComponent()) }
        XCTAssertTrue(FileManager.default.fileExists(atPath: firstURL.path))
    }

    private static func buffer(frames: AVAudioFrameCount) throws -> AVAudioPCMBuffer {
        guard let format = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: 16_000, channels: 1, interleaved: false),
              let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames),
              let channel = buffer.floatChannelData?[0] else {
            throw SpeechAudioGenerationError.synthesisFailed
        }
        buffer.frameLength = frames
        channel.initialize(repeating: 0, count: Int(frames))
        return buffer
    }
}
