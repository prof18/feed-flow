import Foundation

enum SpeechAudioGenerationError: Error {
    case voiceUnavailable
    case emptyAudio
    case outputTooLarge
    case timedOut
    case synthesisFailed
}
