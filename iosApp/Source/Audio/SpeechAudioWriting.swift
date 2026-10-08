import AVFoundation

@MainActor
protocol SpeechAudioWriting: AnyObject {
    func append(_ buffer: AVAudioPCMBuffer) throws
    func finish() throws
}
