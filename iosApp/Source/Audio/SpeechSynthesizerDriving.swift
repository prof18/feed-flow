import AVFoundation

@MainActor
protocol SpeechSynthesizerDriving: AnyObject {
    func synthesize(_ text: String, receive: @escaping @MainActor (AVAudioPCMBuffer) throws -> Void) async throws -> Bool
    func stop()
}
