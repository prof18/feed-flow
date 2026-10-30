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

                Text(feedFlowStrings.loadingFeedMessage(feedRefreshCounter))
                    .font(.body)
            } else {
                Text(feedFlowStrings.loadingFeedMessage("..."))
                    .font(.body)
            }
        } else if keepsSpace {
            Text(feedFlowStrings.loadingFeedMessage("..."))
                .font(.body)
                .hidden()
                .accessibilityHidden(true)
        }
    }
}
