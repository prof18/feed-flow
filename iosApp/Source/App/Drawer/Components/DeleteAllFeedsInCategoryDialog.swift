import FeedFlowKit
import SwiftUI

struct DeleteAllFeedsInCategoryDialog: View {
    @Binding var isPresented: Bool
    @Binding var categoryToDeleteAllFeeds: String?
    let onDeleteAllFeeds: (String) -> Void

    var body: some View {
        EmptyView()
            .alert(feedFlowStrings.deleteAllFeedsConfirmationTitle, isPresented: $isPresented) {
                Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) {
                    categoryToDeleteAllFeeds = nil
                }
                Button(feedFlowStrings.confirmButton, role: .destructive) {
                    if let id = categoryToDeleteAllFeeds {
                        onDeleteAllFeeds(id)
                    }
                    categoryToDeleteAllFeeds = nil
                }
            } message: {
                Text(feedFlowStrings.deleteAllFeedsConfirmationMessage)
            }
    }
}
