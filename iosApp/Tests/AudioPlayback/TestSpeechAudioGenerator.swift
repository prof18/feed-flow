import Foundation

@MainActor
final class TestSpeechAudioGenerator: SpeechAudioGenerating {
    private(set) var requests: [[String]] = []
    private var continuations: [CheckedContinuation<URL, Error>?] = []

    func generate(segments: [String]) async throws -> URL {
        requests.append(segments)
        continuations.append(nil)
        let index = requests.count - 1
        return try await withCheckedThrowingContinuation { continuations[index] = $0 }
    }

    func complete(request index: Int, with url: URL) {
        guard continuations.indices.contains(index), let continuation = continuations[index] else { return }
        continuations[index] = nil
        continuation.resume(returning: url)
    }

    func fail(request index: Int, with error: Error = SpeechAudioGenerationError.synthesisFailed) {
        guard continuations.indices.contains(index), let continuation = continuations[index] else { return }
        continuations[index] = nil
        continuation.resume(throwing: error)
    }

    func waitForRequestCount(_ count: Int) async throws {
        for _ in 0 ..< 10_000 {
            if requests.count >= count { return }
            await Task.yield()
        }
        throw SpeechAudioGenerationError.timedOut
    }
}
