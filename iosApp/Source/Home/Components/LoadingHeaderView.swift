import FeedFlowKit
import SwiftUI

struct LoadingHeaderView: View {
    let loadingState: FeedUpdateStatus?
    let showLoading: Bool

    var body: some View {
        if let feedCount = loadingState?.refreshedFeedCount,
           let totalFeedCount = loadingState?.totalFeedCount {
            if showLoading {
                if feedCount > 0 && totalFeedCount > 0 {
                    let feedRefreshCounter = "\(feedCount)/\(totalFeedCount)"

                    Text(feedFlowStrings.loadingFeedMessage(feedRefreshCounter))
                        .font(.body)
                } else {
                    Text(feedFlowStrings.loadingFeedMessage("..."))
                        .font(.body)
                }
            }
        }
    }
}
