import AVFoundation

@MainActor
protocol SpeechSynthesizerDriving: AnyObject {
    func prepare(languageTag: String?) async throws
    func synthesize(_ text: String, receive: @escaping @MainActor (AVAudioPCMBuffer) throws -> Void) async throws -> Bool
    func stop()
}
