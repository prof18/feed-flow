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
                }.onTapGesture {
                    isReaderModeEnabled.toggle()
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.readerModeToggle)

                Toggle(isOn: $isSaveReaderModeContentEnabled) {
                    Text(feedFlowStrings.settingsSaveReaderModeContent)
                }.onTapGesture {
                    isSaveReaderModeContentEnabled.toggle()
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
                }.onTapGesture {
                    isMarkReadWhenScrollingEnabled.toggle()
                }

                Toggle(isOn: $isShowReadItemEnabled) {
                    Text(feedFlowStrings.settingsToggleShowReadArticles)
                }.onTapGesture {
                    isShowReadItemEnabled.toggle()
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.showReadToggle)

                Toggle(isOn: $isHideReadItemsEnabled) {
                    Text(feedFlowStrings.settingsHideReadItems)
                }.onTapGesture {
                    isHideReadItemsEnabled.toggle()
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.hideReadToggle)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }
}

private enum ReadingBehaviorAccessibilityIdentifiers {
    static let hideReadToggle = "reading_behavior_hide_read"
    static let readerModeToggle = "reading_behavior_reader_mode"
    static let showReadToggle = "reading_behavior_show_read"
}
