import FeedFlowKit
import SwiftUI

struct ReadingBehaviorScreenContent: View {
    @Bindable var browserSelector: BrowserSelector
    @Binding var articleOpenMode: ArticleOpenMode
    @Binding var isSaveReaderModeContentEnabled: Bool
    @Binding var isPrefetchArticleContentEnabled: Bool
    @Binding var isKleadParserEnabled: Bool
    @Binding var isMarkReadWhenScrollingEnabled: Bool
    @Binding var isShowReadItemEnabled: Bool
    @Binding var isHideReadItemsEnabled: Bool

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                Picker(
                    selection: $browserSelector.selectedBrowser,
                    content: {
                        ForEach(browserSelector.browsers, id: \.self) { browser in
                            Text(browser.name).tag(browser as Browser?)
                        }
                    },
                    label: {
                        Text(feedFlowStrings.browserSelectionButton)
                    }
                )

                NavigationLink {
                    ArticleOpenModeSelectionScreen(articleOpenMode: $articleOpenMode)
                } label: {
                    HStack {
                        Text(feedFlowStrings.articleOpenMode)

                        Spacer()

                        Text(articleOpenModePreview)
                            .foregroundStyle(.secondary)
                    }
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.articleOpenModePicker)

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

                SettingToggleItem(
                    isOn: $isKleadParserEnabled,
                    title: feedFlowStrings.settingsUseNewArticleParser,
                    confirmationDialog: ConfirmationDialogConfig(
                        title: feedFlowStrings.settingsNewReaderModeEngineConfirmationTitle,
                        message: feedFlowStrings.settingsNewReaderModeEngineConfirmationMessage
                    )
                )
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.newParserToggle)

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
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.hideReadToggle)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }

    private var articleOpenModePreview: String {
        switch articleOpenMode {
        case .fullArticle:
            feedFlowStrings.articleOpenModeFullArticleShort
        case .feedContent:
            feedFlowStrings.articleOpenModeFeedShort
        case .internalBrowser:
            feedFlowStrings.linkOpeningPreferenceInternalBrowser
        default:
            feedFlowStrings.linkOpeningPreferencePreferredBrowser
        }
    }
}
