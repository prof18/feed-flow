import Foundation

@MainActor
protocol SpeechAudioGenerating {
    func generate(segments: [String]) async throws -> URL
}
