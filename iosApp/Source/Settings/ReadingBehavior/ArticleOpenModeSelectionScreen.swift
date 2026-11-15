import FeedFlowKit
import SwiftUI

struct ArticleOpenModeSelectionScreen: View {
    @Environment(\.dismiss) private var dismiss

    @Binding var articleOpenMode: ArticleOpenMode
    let browsers: [Browser]
    /// nil while a reader mode is selected.
    let selectedArticleBrowserId: String?
    let onBrowserSelected: (String) -> Void

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
                ForEach(browsers, id: \.id) { browser in
                    browserRow(browser)
                }
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
        let isSelected = selectedArticleBrowserId == nil && articleOpenMode == mode
        return Button {
            articleOpenMode = mode
            dismiss()
        } label: {
            rowLabel(title: title, subtitle: subtitle, isSelected: isSelected)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(accessibilityIdentifier)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func browserRow(_ browser: Browser) -> some View {
        let isSelected = selectedArticleBrowserId == browser.id
        let identifier = browser.id == BrowserIds.shared.IN_APP_BROWSER
            ? ReadingBehaviorAccessibilityIdentifiers.articleOpenModeInternalBrowserOption
            : ReadingBehaviorAccessibilityIdentifiers.articleOpenModeBrowserOption(browser.id)
        return Button {
            onBrowserSelected(browser.id)
            dismiss()
        } label: {
            rowLabel(title: browser.name, subtitle: nil, isSelected: isSelected)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(identifier)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func rowLabel(title: String, subtitle: String?, isSelected: Bool) -> some View {
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

            if isSelected {
                Image(systemName: "checkmark")
            }
        }
        .contentShape(.rect)
        .foregroundStyle(.primary)
    }
}
