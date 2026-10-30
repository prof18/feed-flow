import AVFoundation
import Foundation

@MainActor
final class TestSpeechAudioWriter: SpeechAudioWriting {
    private let writer: NativeSpeechAudioWriter
    private(set) var appendCount = 0

    init(url: URL, format: AVAudioFormat) throws {
        writer = try NativeSpeechAudioWriter(url: url, format: format)
    }

    func append(_ buffer: AVAudioPCMBuffer) throws {
        try writer.append(buffer)
        appendCount += 1
    }

    func finish() throws { try writer.finish() }
}
