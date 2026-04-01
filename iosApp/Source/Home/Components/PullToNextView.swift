import FeedFlowKit
import SwiftUI

struct NextFeedButton: View {
    let title: String
    let onNavigateNext: () -> Void

    var body: some View {
        Button(action: onNavigateNext) {
            HStack(spacing: Spacing.xsmall) {
                Text(title)
                Image(systemName: "chevron.right")
                    .font(.caption2)
            }
        }
        .buttonStyle(.borderless)
        .frame(maxWidth: .infinity)
        .listRowSeparator(.hidden)
    }
}
