import FeedFlowKit
import SwiftUI

struct StorageScreenContent: View {
    @Binding var autoDeletePeriod: AutoDeletePeriod
    let onClearDownloadedArticles: () -> Void

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                Picker(selection: $autoDeletePeriod) {
                    Text(feedFlowStrings.settingsAutoDeletePeriodDisabled)
                        .tag(AutoDeletePeriod.disabled)
                    Text(feedFlowStrings.settingsAutoDeletePeriodOneDay)
                        .tag(AutoDeletePeriod.oneDay)
                    Text(feedFlowStrings.settingsAutoDeletePeriodOneWeek)
                        .tag(AutoDeletePeriod.oneWeek)
                    Text(feedFlowStrings.settingsAutoDeletePeriodTwoWeeks)
                        .tag(AutoDeletePeriod.twoWeeks)
                    Text(feedFlowStrings.settingsAutoDeletePeriodOneMonth)
                        .tag(AutoDeletePeriod.oneMonth)
                } label: {
                    Text(feedFlowStrings.settingsAutoDelete)
                }
                .accessibilityIdentifier(StorageAccessibilityIdentifiers.autoDelete)

                ConfirmationButton(
                    title: feedFlowStrings.settingsClearDownloadedArticles,
                    dialogTitle: feedFlowStrings.settingsClearDownloadedArticlesDialogTitle,
                    dialogMessage: feedFlowStrings.settingsClearDownloadedArticlesDialogMessage,
                    onConfirm: onClearDownloadedArticles
                )
                .accessibilityIdentifier(StorageAccessibilityIdentifiers.clearDownloaded)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }
}
