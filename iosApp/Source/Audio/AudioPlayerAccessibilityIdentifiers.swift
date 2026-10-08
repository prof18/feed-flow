import Foundation

enum AudioPlayerAccessibilityIdentifiers {
    static let player = "audio_player"
    static let expand = "reader_audio_player_expand"
    static let play = "audio_play"
    static let pause = "audio_pause"
    static let close = "audio_close_player"
    static let seek = "audio_seek"
    static let position = "audio_position"
    static let external = "reader_audio_player_external"
    static let speed = "audio_playback_speed"
    static let nowPlaying = "audio_now_playing"
    static let returnToEpisode = "audio_return_to_episode"
    static let miniPause = "audio_mini_pause"
    static let miniPlay = "audio_mini_play"
    static let dockReturnToEpisode = "audio_dock_return_to_episode"
    static func speedOption(_ speed: Double) -> String {
        "audio_playback_speed_\(speedLabel(speed))"
    }

    private static func speedLabel(_ speed: Double) -> String {
        speed == speed.rounded() ? String(Int(speed)) : String(speed).replacingOccurrences(of: ".", with: "_")
    }
}
