import FeedFlowKit
import SwiftUI

struct SyncAndStorageScreenContent: View {
    @Binding var autoDeletePeriod: AutoDeletePeriod
    @Binding var refreshFeedsOnLaunch: Bool
    @Binding var showRssParsingErrors: Bool
    let onClearDownloadedArticles: () -> Void

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                Toggle(isOn: $refreshFeedsOnLaunch) {
                    Text(feedFlowStrings.settingsRefreshFeedsOnLaunch)
                }

                Toggle(isOn: $showRssParsingErrors) {
                    Text(feedFlowStrings.settingsShowRssParsingErrors)
                }

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

                ConfirmationButton(
                    title: feedFlowStrings.settingsClearDownloadedArticles,
                    dialogTitle: feedFlowStrings.settingsClearDownloadedArticlesDialogTitle,
                    dialogMessage: feedFlowStrings.settingsClearDownloadedArticlesDialogMessage,
                    onConfirm: onClearDownloadedArticles
                )
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }
}
