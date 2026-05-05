import FeedFlowKit
import SwiftUI

struct DeleteFeedSourceDialog: View {
    @Binding var isPresented: Bool
    @Binding var feedToDelete: FeedSource?
    let onDelete: (FeedSource) -> Void

    var body: some View {
        EmptyView()
            .alert(feedFlowStrings.deleteFeedConfirmationTitle, isPresented: $isPresented) {
                Button(feedFlowStrings.cancelButton, role: .cancel) {
                    feedToDelete = nil
                }
                Button(feedFlowStrings.deleteFeedButton, role: .destructive) {
                    if let feed = feedToDelete {
                        onDelete(feed)
                    }
                    feedToDelete = nil
                }
            } message: {
                Text(feedFlowStrings.deleteFeedConfirmationMessage)
            }
    }
}
