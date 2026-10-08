import Foundation

struct AudioEpisode: Equatable {
    let itemId: String
    let url: URL
    let title: String
    let subtitle: String?
    let artworkURL: URL?
}
