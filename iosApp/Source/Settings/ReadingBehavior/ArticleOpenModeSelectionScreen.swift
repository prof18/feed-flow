import FeedFlowKit
import SwiftUI

struct ArticleOpenModeSelectionScreen: View {
    @Environment(\.dismiss) private var dismiss

    @Binding var articleOpenMode: ArticleOpenMode

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section(feedFlowStrings.articleOpenModeSectionReader) {
                selectionRow(
                    title: feedFlowStrings.readerContentSourceWeb,
                    subtitle: feedFlowStrings.readerContentSourceWebSubtitle,
                    mode: .fullArticle,
                    accessibilityIdentifier: ReadingBehaviorAccessibilityIdentifiers.articleOpenModeFullArticleOption
                )
                selectionRow(
                    title: feedFlowStrings.readerContentSourceFeed,
                    subtitle: feedFlowStrings.readerContentSourceFeedSubtitle,
                    mode: .feedContent,
                    accessibilityIdentifier: ReadingBehaviorAccessibilityIdentifiers.articleOpenModeFeedContentOption
                )
            }

            Section(feedFlowStrings.articleOpenModeSectionBrowser) {
                selectionRow(
                    title: feedFlowStrings.linkOpeningPreferenceInternalBrowser,
                    mode: .internalBrowser,
                    accessibilityIdentifier: ReadingBehaviorAccessibilityIdentifiers
                        .articleOpenModeInternalBrowserOption
                )
                selectionRow(
                    title: feedFlowStrings.linkOpeningPreferencePreferredBrowser,
                    mode: .preferredBrowser,
                    accessibilityIdentifier: ReadingBehaviorAccessibilityIdentifiers
                        .articleOpenModePreferredBrowserOption
                )
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.articleOpenMode))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func selectionRow(
        title: String,
        subtitle: String? = nil,
        mode: ArticleOpenMode,
        accessibilityIdentifier: String
    ) -> some View {
        Button {
            articleOpenMode = mode
            dismiss()
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)

                    if let subtitle {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Spacer()

                if articleOpenMode == mode {
                    Image(systemName: "checkmark")
                }
            }
            .contentShape(.rect)
            .foregroundStyle(.primary)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(accessibilityIdentifier)
        .accessibilityAddTraits(articleOpenMode == mode ? .isSelected : [])
    }
}
