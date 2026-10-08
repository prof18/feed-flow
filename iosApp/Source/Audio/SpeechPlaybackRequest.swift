import CryptoKit
import Foundation

struct SpeechPlaybackRequest {
    let title: String?
    let content: String
    let episode: AudioEpisode

    init(itemId: String, title: String?, displayTitle: String? = nil, content: String, subtitle: String?, artworkURL: URL?) {
        self.title = title
        self.content = content
        let source = title.map { Data(($0 + "\u{0000}" + content).utf8) }
            ?? Data([0xff] + Array(("\u{0000}" + content).utf8))
        let digest = SHA256.hash(data: source)
            .map { String(format: "%02x", $0) }.joined()
        episode = AudioEpisode(
            itemId: itemId,
            url: URL(fileURLWithPath: "/reader-speech-pending"),
            title: displayTitle ?? title ?? "",
            subtitle: subtitle,
            artworkURL: artworkURL,
            kind: .speech,
            contentKey: digest
        )
    }
}
