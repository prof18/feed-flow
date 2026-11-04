import FeedFlowKit
import SwiftUI

struct LoadingHeaderView: View {
    let loadingState: FeedUpdateStatus?
    let showLoading: Bool
    /// Keeps the header's height once loading ends, so a list scrolled away from the top doesn't shift.
    var keepsSpace = false

    var body: some View {
        if showLoading,
           let feedCount = loadingState?.refreshedFeedCount,
           let totalFeedCount = loadingState?.totalFeedCount {
            if feedCount > 0 && totalFeedCount > 0 {
                let feedRefreshCounter = "\(feedCount)/\(totalFeedCount)"

                LoadingHeaderChip(text: feedFlowStrings.loadingFeedMessage(feedRefreshCounter))
            } else {
                LoadingHeaderChip(text: feedFlowStrings.loadingFeedMessage("..."))
            }
        } else if keepsSpace {
            LoadingHeaderChip(text: feedFlowStrings.loadingFeedMessage("..."))
                .hidden()
                .accessibilityHidden(true)
        }
    }
}

/// The header floats over the feed list, so it needs its own background to stay readable over articles.
private struct LoadingHeaderChip: View {
    let text: String

    var body: some View {
        let label = Text(text)
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, Spacing.regular)
            .padding(.vertical, Spacing.small)

        if #available(iOS 26.0, *) {
            label.glassEffect(.regular, in: .capsule)
        } else {
            label
                .background(.regularMaterial)
                .clipShape(Capsule())
                .shadow(radius: 5)
        }
    }
}
