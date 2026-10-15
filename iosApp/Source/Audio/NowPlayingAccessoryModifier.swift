import SwiftUI

struct NowPlayingAccessoryModifier: ViewModifier {
    @Environment(AudioPlaybackController.self)
    private var playback

    var route: CommonViewRoute?
    var isEnabled = true
    let onOpenEpisode: (AudioEpisode) -> Void

    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.safeAreaBar(edge: .bottom, spacing: 0) {
                indicator
            }
        } else {
            content.safeAreaInset(edge: .bottom, spacing: 0) {
                indicator
            }
        }
    }

    @ViewBuilder private var indicator: some View {
        if isEnabled, playback.episode != nil, isVisible {
            NowPlayingIndicator(onOpenEpisode: onOpenEpisode)
                .padding(.vertical, 8)
        }
    }

    private var isVisible: Bool {
        switch route {
        case .readerMode, .deepLinkFeed:
            return false
        default:
            return true
        }
    }
}
