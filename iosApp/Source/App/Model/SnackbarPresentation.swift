import Foundation
import Observation

@Observable
final class SnackbarPresentation {
    private var surfaces: [(id: UUID, depth: Int)] = []
    private(set) var messageID: UUID?
    private(set) var isExiting = false
    private(set) var deadline: ContinuousClock.Instant?

    var frontmostSurface: UUID? {
        surfaces.reversed().max { $0.depth < $1.depth }?.id
    }

    func register(_ id: UUID, depth: Int) {
        guard !surfaces.contains(where: { $0.id == id }) else { return }
        surfaces.append((id, depth))
    }

    func unregister(_ id: UUID) {
        surfaces.removeAll { $0.id == id }
    }

    func show(_ id: UUID) -> Bool {
        guard messageID == nil else { return false }
        messageID = id
        deadline = ContinuousClock.now + .seconds(5)
        return true
    }

    func beginDismissal(_ id: UUID) -> Bool {
        guard messageID == id, !isExiting else { return false }
        isExiting = true
        deadline = ContinuousClock.now + .milliseconds(300)
        return true
    }

    func completeDismissal(_ id: UUID) -> Bool {
        guard messageID == id, isExiting else { return false }
        messageID = nil
        isExiting = false
        deadline = nil
        return true
    }
}
