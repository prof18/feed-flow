import AVFoundation
import Foundation

@MainActor
final class FirstVoiceCallGate {
    let gate = AsyncVoiceAvailabilityGate()
    private var calls = 0

    func check() async -> Bool {
        calls += 1
        if calls == 1 { return await gate.wait() }
        return true
    }

    func resumeFirstCall(_ value: Bool) { gate.resume(value) }
}
