import FeedFlowKit
import SwiftUI

struct SyncAndRefreshScreen: View {
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
        SyncAndRefreshScreenContent(
            refreshFeedsOnLaunch: Binding(
                get: { settingsState.refreshFeedsOnLaunch },
                set: { vmStoreOwner.instance.updateRefreshFeedsOnLaunch(enabled: $0) }
            )
        )
        .navigationTitle(Text(feedFlowStrings.settingsSyncAndRefresh))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            for await state in vmStoreOwner.instance.state {
                self.settingsState = state
            }
        }
    }
}
