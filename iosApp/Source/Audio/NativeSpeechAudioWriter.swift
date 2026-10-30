import AVFoundation
import Foundation

@MainActor
final class NativeSpeechAudioWriter: SpeechAudioWriting {
    private var file: AVAudioFile?

    init(url: URL, format: AVAudioFormat) throws {
        file = try AVAudioFile(forWriting: url, settings: format.settings, commonFormat: format.commonFormat, interleaved: format.isInterleaved)
    }

    func append(_ buffer: AVAudioPCMBuffer) throws {
        guard let file else { throw SpeechAudioGenerationError.synthesisFailed }
        try file.write(from: buffer)
    }

    func finish() throws {
        file = nil
    }
}
