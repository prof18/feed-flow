import FeedFlowKit
import SwiftUI

struct ReadingBehaviorScreenContent: View {
    @Bindable var browserSelector: BrowserSelector
    @Binding var isReaderModeEnabled: Bool
    @Binding var isSaveReaderModeContentEnabled: Bool
    @Binding var isPrefetchArticleContentEnabled: Bool
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

                Toggle(isOn: $isReaderModeEnabled) {
                    Text(feedFlowStrings.settingsReaderMode)
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.readerModeToggle)

                Toggle(isOn: $isSaveReaderModeContentEnabled) {
                    Text(feedFlowStrings.settingsSaveReaderModeContent)
                }

                SettingToggleItem(
                    isOn: $isPrefetchArticleContentEnabled,
                    title: feedFlowStrings.settingsPrefetchArticleContent,
                    confirmationDialog: ConfirmationDialogConfig(
                        title: feedFlowStrings.settingsPrefetchArticleContent,
                        message: feedFlowStrings.settingsPrefetchArticleContentWarning
                    )
                )

                Toggle(isOn: $isMarkReadWhenScrollingEnabled) {
                    Text(feedFlowStrings.toggleMarkReadWhenScrolling)
                }

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
}
