import FeedFlowKit
import SwiftUI

struct NewArticlesPillView: View {
    let count: Int
    let onTap: () -> Void

    private let maxCount = 40

    private var label: String {
        count >= maxCount ? "\(maxCount)+" : "\(count)"
    }

    var body: some View {
        Group {
            if #available(iOS 26.0, *) {
                Button(action: onTap) {
                    pillLabel
                }
                .buttonStyle(.glass)
            } else {
                Button(action: onTap) {
                    pillLabel
                        .padding(.horizontal, Spacing.regular)
                        .padding(.vertical, Spacing.small)
                        .background(.regularMaterial)
                        .clipShape(Capsule())
                        .shadow(radius: 5)
                }
                .buttonStyle(.plain)
            }
        }
        .accessibilityIdentifier(NewArticlesPillAccessibilityIdentifiers.pill)
    }

    private var pillLabel: some View {
        Label(feedFlowStrings.newArticlesButton(label), systemImage: "arrow.up")
            .font(.subheadline.weight(.medium))
            .foregroundStyle(.primary)
    }
}
