import SwiftUI

struct EmptyDetailPane: View {
    var body: some View {
        VStack(spacing: Spacing.medium) {
            Image(systemName: "newspaper")
                .font(.system(size: 56))
                .foregroundStyle(.secondary)

            Text(feedFlowStrings.selectArticleToRead)
                .font(.title3)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
