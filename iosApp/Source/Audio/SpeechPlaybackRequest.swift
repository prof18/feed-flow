import CryptoKit
import Foundation

struct SpeechPlaybackRequest {
    let content: String
    let episode: AudioEpisode

    init(itemId: String, title: String, content: String, subtitle: String?, artworkURL: URL?) {
        self.content = content
        let digest = SHA256.hash(data: Data((title + "\u{0000}" + content).utf8))
            .map { String(format: "%02x", $0) }.joined()
        episode = AudioEpisode(
            itemId: itemId,
            url: URL(fileURLWithPath: "/reader-speech-pending"),
            title: title,
            subtitle: subtitle,
            artworkURL: artworkURL,
            kind: .speech,
            contentKey: digest
        )
    }
}
