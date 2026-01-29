import FeedFlowKit
import SwiftUI

struct SyncAndStorageScreen: View {
    @Environment(AppState.self) 
    private var appState
    
    @StateObject private var vmStoreOwner = VMStoreOwner<SyncAndStorageSettingsViewModel>(
        Deps.shared.getSyncAndStorageSettingsViewModel()
    )
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = SyncAndStorageState(
        syncPeriod: .oneHour,
        autoDeletePeriod: .disabled,
        refreshFeedsOnLaunch: true,
        showRssParsingErrors: true
    )

    var body: some View {
        @Bindable var appState = appState

        SyncAndStorageScreenContent(
            syncPeriod: Binding(
                get: { settingsState.syncPeriod },
                set: { vmStoreOwner.instance.updateSyncPeriod(period: $0) }
            ),
            autoDeletePeriod: Binding(
                get: { settingsState.autoDeletePeriod },
                set: { vmStoreOwner.instance.updateAutoDeletePeriod(period: $0) }
            ),
            refreshFeedsOnLaunch: Binding(
                get: { settingsState.refreshFeedsOnLaunch },
                set: { vmStoreOwner.instance.updateRefreshFeedsOnLaunch(enabled: $0) }
            ),
            showRssParsingErrors: Binding(
                get: { settingsState.showRssParsingErrors },
                set: { vmStoreOwner.instance.updateShowRssParsingErrors(enabled: $0) }
            ),
            onClearDownloadedArticles: {
                vmStoreOwner.instance.clearDownloadedArticleContent()
            }
        )
        .navigationTitle(Text(feedFlowStrings.settingsSyncAndStorage))
        .navigationBarTitleDisplayMode(.inline)
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in vmStoreOwner.instance.state {
                self.settingsState = state
            }
        }
    }
}
