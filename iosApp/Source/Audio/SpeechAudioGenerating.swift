import Foundation

@MainActor
protocol SpeechAudioGenerating {
    func generate(segments: [String], languageTag: String?) async throws -> URL
}
