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
                    Label(feedFlowStrings.settingsRefreshFeedsOnLaunch, systemImage: "arrow.triangle.2.circlepath")
                }

                Toggle(isOn: $showRssParsingErrors) {
                    Label(feedFlowStrings.settingsShowRssParsingErrors, systemImage: "exclamationmark.triangle")
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
                    Label(feedFlowStrings.settingsAutoDelete, systemImage: "arrow.3.trianglepath")
                }

                ConfirmationButton(
                    title: feedFlowStrings.settingsClearDownloadedArticles,
                    systemImage: "trash",
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
