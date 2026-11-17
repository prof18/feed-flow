import FeedFlowKit
import SwiftUI

struct StorageScreenContent: View {
    @Binding var autoDeletePeriod: AutoDeletePeriod
    @Binding var isSaveReaderModeContentEnabled: Bool
    @Binding var isPrefetchArticleContentEnabled: Bool
    let onClearDownloadedArticles: () -> Void

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                NavigationLink {
                    AutoDeletePeriodSelectionScreen(autoDeletePeriod: $autoDeletePeriod)
                } label: {
                    LabeledContent(
                        feedFlowStrings.settingsAutoDelete,
                        value: AutoDeletePeriodSelectionScreen.label(for: autoDeletePeriod, strings: feedFlowStrings)
                    )
                }
                .accessibilityIdentifier(StorageAccessibilityIdentifiers.autoDelete)
            } footer: {
                Text(feedFlowStrings.settingsAutoDeleteDescription)
            }

            Section {
                SettingToggleItem(
                    isOn: $isSaveReaderModeContentEnabled,
                    title: feedFlowStrings.settingsSaveReaderModeContent
                )
                .accessibilityIdentifier(StorageAccessibilityIdentifiers.saveContentToggle)
            } header: {
                Text(feedFlowStrings.settingsOfflineCopiesSectionTitle)
            } footer: {
                Text(feedFlowStrings.settingsSaveReaderModeContentDescription)
            }

            Section {
                SettingToggleItem(
                    isOn: $isPrefetchArticleContentEnabled,
                    title: feedFlowStrings.settingsPrefetchArticleContent,
                    confirmationDialog: ConfirmationDialogConfig(
                        title: feedFlowStrings.settingsPrefetchArticleContent,
                        message: feedFlowStrings.settingsPrefetchArticleContentWarning
                    )
                )
                .accessibilityIdentifier(StorageAccessibilityIdentifiers.prefetchContentToggle)
            } footer: {
                Text(feedFlowStrings.settingsPrefetchArticleContentDescription)
            }

            Section {
                ConfirmationButton(
                    title: feedFlowStrings.settingsClearDownloadedArticles,
                    dialogTitle: feedFlowStrings.settingsClearDownloadedArticlesDialogTitle,
                    dialogMessage: feedFlowStrings.settingsClearDownloadedArticlesDialogMessage,
                    onConfirm: onClearDownloadedArticles
                )
                .accessibilityIdentifier(StorageAccessibilityIdentifiers.clearDownloaded)
            } footer: {
                Text(feedFlowStrings.settingsClearDownloadedArticlesDescription)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }
}
