import Foundation

final class TestAudioPlaybackPositionStore: AudioPlaybackPositionStore {
    var positions: [String: Int64] = [:]

    func position(for itemId: String) -> Int64 {
        positions[itemId] ?? 0
    }

    func save(itemId: String, positionMs: Int64, durationMs _: Int64) {
        positions[itemId] = positionMs
    }
}
