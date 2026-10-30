import FeedFlowKit
import Foundation

struct RepositoryAudioPlaybackPositionStore: AudioPlaybackPositionStore {
    let repository: AudioPlaybackPositionRepository

    func position(for itemId: String) -> Int64 {
        repository.getPosition(itemId: itemId)
    }

    func save(itemId: String, positionMs: Int64, durationMs: Int64) {
        repository.savePosition(itemId: itemId, positionMs: positionMs, durationMs: durationMs)
    }
}
