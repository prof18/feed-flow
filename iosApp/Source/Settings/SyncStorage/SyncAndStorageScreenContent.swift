import FeedFlowKit
import SwiftUI

struct SyncAndStorageScreenContent: View {
    @Binding var syncPeriod: SyncPeriod
    @Binding var autoDeletePeriod: AutoDeletePeriod
    let onClearDownloadedArticles: () -> Void

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                Picker(selection: $syncPeriod) {
                    Text(feedFlowStrings.settingsSyncPeriodNever)
                        .tag(SyncPeriod.never)
                    Text(feedFlowStrings.settingsSyncPeriodFifteenMinutes)
                        .tag(SyncPeriod.fifteenMinutes)
                    Text(feedFlowStrings.settingsSyncPeriodThirtyMinutes)
                        .tag(SyncPeriod.thirtyMinutes)
                    Text(feedFlowStrings.settingsSyncPeriodOneHour)
                        .tag(SyncPeriod.oneHour)
                    Text(feedFlowStrings.settingsSyncPeriodTwoHours)
                        .tag(SyncPeriod.twoHours)
                    Text(feedFlowStrings.settingsSyncPeriodSixHours)
                        .tag(SyncPeriod.sixHours)
                    Text(feedFlowStrings.settingsSyncPeriodTwelveHours)
                        .tag(SyncPeriod.twelveHours)
                    Text(feedFlowStrings.settingsSyncPeriodOneDay)
                        .tag(SyncPeriod.oneDay)
                } label: {
                    Label(feedFlowStrings.settingsSyncPeriod, systemImage: "arrow.clockwise")
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
