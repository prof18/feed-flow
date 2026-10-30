import AVFoundation
import Foundation

@MainActor
final class AsyncVoiceAvailabilityGate {
    private var continuation: CheckedContinuation<Bool, Never>?
    private var pendingValue: Bool?
    private(set) var isWaiting = false

    func wait() async -> Bool {
        if let pendingValue {
            self.pendingValue = nil
            return pendingValue
        }
        return await withCheckedContinuation { continuation in
            self.continuation = continuation
            isWaiting = true
        }
    }

    func resume(_ value: Bool) {
        guard let continuation else {
            pendingValue = value
            return
        }
        self.continuation = nil
        isWaiting = false
        continuation.resume(returning: value)
    }
}
