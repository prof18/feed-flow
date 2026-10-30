import Foundation

protocol AudioPlaybackPositionStore {
    func position(for itemId: String) -> Int64
    func save(itemId: String, positionMs: Int64, durationMs: Int64)
}
