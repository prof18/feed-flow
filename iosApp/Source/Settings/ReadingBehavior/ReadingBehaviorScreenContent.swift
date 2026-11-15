import FeedFlowKit
import SwiftUI

struct ReadingBehaviorScreenContent: View {
    @Bindable var browserSelector: BrowserSelector
    @Binding var articleOpenMode: ArticleOpenMode
    let onArticleBrowserSelected: (String) -> Void
    @Binding var isSaveReaderModeContentEnabled: Bool
    @Binding var isPrefetchArticleContentEnabled: Bool
    @Binding var isMarkReadWhenScrollingEnabled: Bool
    @Binding var isShowReadItemEnabled: Bool
    @Binding var isHideReadItemsEnabled: Bool

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                NavigationLink {
                    ArticleOpenModeSelectionScreen(
                        articleOpenMode: $articleOpenMode,
                        browsers: browserSelector.browsers,
                        selectedArticleBrowserId: selectedArticleBrowserId,
                        onBrowserSelected: onArticleBrowserSelected
                    )
                } label: {
                    HStack {
                        titleWithSubtitle(
                            title: feedFlowStrings.articleOpenMode,
                            subtitle: feedFlowStrings.articleOpenModeSubtitle
                        )

                        Spacer()

                        Text(articleOpenModePreview)
                            .foregroundStyle(.secondary)
                    }
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.articleOpenModePicker)

                NavigationLink {
                    LinksBrowserSelectionScreen(browserSelector: browserSelector)
                } label: {
                    HStack {
                        titleWithSubtitle(
                            title: feedFlowStrings.linksBrowserTitle,
                            subtitle: feedFlowStrings.linksBrowserSubtitle
                        )

                        Spacer()

                        Text(browserSelector.selectedBrowser?.name ?? feedFlowStrings.inAppBrowser)
                            .foregroundStyle(.secondary)
                    }
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.linksBrowserPicker)

                Toggle(isOn: $isSaveReaderModeContentEnabled) {
                    Text(feedFlowStrings.settingsSaveReaderModeContent)
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.saveContentToggle)

                SettingToggleItem(
                    isOn: $isPrefetchArticleContentEnabled,
                    title: feedFlowStrings.settingsPrefetchArticleContent,
                    confirmationDialog: ConfirmationDialogConfig(
                        title: feedFlowStrings.settingsPrefetchArticleContent,
                        message: feedFlowStrings.settingsPrefetchArticleContentWarning
                    )
                )
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.prefetchContentToggle)

                Toggle(isOn: $isMarkReadWhenScrollingEnabled) {
                    Text(feedFlowStrings.toggleMarkReadWhenScrolling)
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.markReadWhenScrollingToggle)

                Toggle(isOn: $isShowReadItemEnabled) {
                    Text(feedFlowStrings.settingsToggleShowReadArticles)
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.showReadToggle)

                Toggle(isOn: $isHideReadItemsEnabled) {
                    Text(feedFlowStrings.settingsHideReadItems)
                }
                .disabled(isMarkReadWhenScrollingEnabled)
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.hideReadToggle)

                if isMarkReadWhenScrollingEnabled {
                    Text(feedFlowStrings.settingsHideReadItemsDisabledWhileScrolling)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }

    private var selectedArticleBrowserId: String? {
        switch articleOpenMode {
        case .internalBrowser:
            BrowserIds.shared.IN_APP_BROWSER
        case .preferredBrowser:
            browserSelector.articleBrowserId()
        default:
            nil
        }
    }

    private var articleOpenModePreview: String {
        switch articleOpenMode {
        case .fullArticle:
            feedFlowStrings.articleOpenModeFullArticleShort
        case .feedContent:
            feedFlowStrings.articleOpenModeFeedShort
        case .internalBrowser:
            feedFlowStrings.inAppBrowser
        default:
            browserSelector.articleBrowserId() == BrowserIds.shared.IN_APP_BROWSER
                ? feedFlowStrings.inAppBrowser
                : browserSelector.articleBrowserName()
        }
    }

    private func titleWithSubtitle(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
            Text(subtitle)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}
