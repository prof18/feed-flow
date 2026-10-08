import Foundation

enum AudioPlaybackProgress {
    static func seconds(_ time: Double) -> Double {
        time.isFinite ? max(0, time) : 0
    }

    static func milliseconds(_ time: Double) -> Int64 {
        Int64(min(seconds(time) * 1_000, Double(Int64.max / 2)))
    }

    static func seekPosition(_ position: Double, duration: Double) -> Double {
        let value = seconds(position)
        return duration.isFinite && duration > 0 ? min(value, duration) : value
    }
}
