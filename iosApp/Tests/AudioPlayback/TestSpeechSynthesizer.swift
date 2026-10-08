import AVFoundation
import Foundation

@MainActor
final class TestSpeechSynthesizer: SpeechSynthesizerDriving {
    private(set) var texts: [String] = []
    private(set) var stopped = false
    private var callback: (@MainActor (AVAudioPCMBuffer) throws -> Void)?
    private var continuation: CheckedContinuation<Bool, Error>?
    var lastOutputPath: String?

    func synthesize(_ text: String, receive: @escaping @MainActor (AVAudioPCMBuffer) throws -> Void) async throws -> Bool {
        texts.append(text)
        callback = receive
        return try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation = $0 }
        } onCancel: {
            Task { @MainActor [weak self] in self?.stop() }
        }
    }

    func send(_ buffer: AVAudioPCMBuffer) { try? callback?(buffer) }

    func finishCurrent(with buffer: AVAudioPCMBuffer) {
        try? callback?(buffer)
        if let endBuffer = Self.endBuffer(format: buffer.format) { try? callback?(endBuffer) }
        continuation?.resume(returning: true)
        continuation = nil
    }

    func sendLate(_ buffer: AVAudioPCMBuffer) {
        try? callback?(buffer)
    }

    func stop() {
        stopped = true
        continuation?.resume(throwing: CancellationError())
        continuation = nil
    }

    func waitForCallCount(_ count: Int) async throws {
        for _ in 0 ..< 10_000 {
            if texts.count >= count { return }
            await Task.yield()
        }
        throw SpeechAudioGenerationError.timedOut
    }

    private static func endBuffer(format: AVAudioFormat) -> AVAudioPCMBuffer? {
        AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 0)
    }
}
