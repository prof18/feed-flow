import FeedFlowKit
import SwiftUI

struct ReadingBehaviorScreenContent: View {
    @Bindable var browserSelector: BrowserSelector
    @Binding var isReaderModeEnabled: Bool
    @Binding var isSaveReaderModeContentEnabled: Bool
    @Binding var isPrefetchArticleContentEnabled: Bool
    @Binding var isMarkReadWhenScrollingEnabled: Bool
    @Binding var isShowReadItemEnabled: Bool

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
                        Label(feedFlowStrings.browserSelectionButton, systemImage: "globe")
                    }
                )

                Toggle(isOn: $isReaderModeEnabled) {
                    Label(feedFlowStrings.settingsReaderMode, systemImage: "doc.text")
                }.onTapGesture {
                    isReaderModeEnabled.toggle()
                }

                Toggle(isOn: $isSaveReaderModeContentEnabled) {
                    Label(feedFlowStrings.settingsSaveReaderModeContent, systemImage: "arrow.down.doc")
                }.onTapGesture {
                    isSaveReaderModeContentEnabled.toggle()
                }

                SettingToggleItem(
                    isOn: $isPrefetchArticleContentEnabled,
                    title: feedFlowStrings.settingsPrefetchArticleContent,
                    systemImage: "icloud.and.arrow.down",
                    confirmationDialog: ConfirmationDialogConfig(
                        title: feedFlowStrings.settingsPrefetchArticleContent,
                        message: feedFlowStrings.settingsPrefetchArticleContentWarning
                    )
                )

                Toggle(isOn: $isMarkReadWhenScrollingEnabled) {
                    Label(feedFlowStrings.toggleMarkReadWhenScrolling, systemImage: "book")
                }.onTapGesture {
                    isMarkReadWhenScrollingEnabled.toggle()
                }

                Toggle(isOn: $isShowReadItemEnabled) {
                    Label(feedFlowStrings.settingsToggleShowReadArticles, systemImage: "checkmark.circle")
                }.onTapGesture {
                    isShowReadItemEnabled.toggle()
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }
}

#Preview {
    @Previewable @State var browserSelector = BrowserSelector()
    @Previewable @State var isReaderModeEnabled = true
    @Previewable @State var isSaveReaderModeContentEnabled = false
    @Previewable @State var isPrefetchArticleContentEnabled = false
    @Previewable @State var isMarkReadWhenScrollingEnabled = true
    @Previewable @State var isShowReadItemEnabled = true

    NavigationStack {
        ReadingBehaviorScreenContent(
            browserSelector: browserSelector,
            isReaderModeEnabled: $isReaderModeEnabled,
            isSaveReaderModeContentEnabled: $isSaveReaderModeContentEnabled,
            isPrefetchArticleContentEnabled: $isPrefetchArticleContentEnabled,
            isMarkReadWhenScrollingEnabled: $isMarkReadWhenScrollingEnabled,
            isShowReadItemEnabled: $isShowReadItemEnabled
        )
        .navigationTitle(Text("Reading Behavior"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
