import Foundation

struct AudioEpisode: Equatable {
    let itemId: String
    let url: URL
    let title: String
    let subtitle: String?
    let artworkURL: URL?
    var kind: AudioSourceKind = .podcast
    var contentKey: String?

    var playbackId: String {
        kind == .speech ? "speech:\(itemId):\(contentKey ?? "")" : itemId
    }
}
