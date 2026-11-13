import FeedFlowKit
import SwiftUI

struct StorageScreen: View {
    @StateObject private var vmStoreOwner = VMStoreOwner<SyncAndStorageSettingsViewModel>(
        Deps.shared.getSyncAndStorageSettingsViewModel()
    )
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = SyncAndStorageState(
        syncPeriod: .oneHour,
        backgroundSyncRestrictions: BackgroundSyncRestrictions(syncOnlyOnWifi: false, syncOnlyWhenCharging: false),
        autoDeletePeriod: .disabled,
        refreshFeedsOnLaunch: true
    )

    var body: some View {
        StorageScreenContent(
            autoDeletePeriod: Binding(
                get: { settingsState.autoDeletePeriod },
                set: { vmStoreOwner.instance.updateAutoDeletePeriod(period: $0) }
            ),
            onClearDownloadedArticles: {
                vmStoreOwner.instance.clearDownloadedArticleContent()
            }
        )
        .navigationTitle(Text(feedFlowStrings.settingsStorage))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            for await state in vmStoreOwner.instance.state {
                self.settingsState = state
            }
        }
    }
}
