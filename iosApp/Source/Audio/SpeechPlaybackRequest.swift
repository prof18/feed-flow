import CryptoKit
import Foundation

struct SpeechPlaybackRequest {
    let title: String?
    let content: String
    let languageTag: String?
    let episode: AudioEpisode

    init(
        itemId: String,
        title: String?,
        displayTitle: String? = nil,
        content: String,
        subtitle: String?,
        artworkURL: URL?,
        languageTag: String? = nil
    ) {
        self.title = title
        self.content = content
        self.languageTag = languageTag
        let source = title.map { Data(($0 + "\u{0000}" + content).utf8) }
            ?? Data([0xFF] + Array(("\u{0000}" + content).utf8))
        let languageSource = Data("speech-language-v1\u{0000}".utf8) + source
            + Data(("\u{0000}" + (languageTag ?? "")).utf8)
        let digest = SHA256.hash(data: languageSource)
            .map { String(format: "%02x", $0) }
            .joined()
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
