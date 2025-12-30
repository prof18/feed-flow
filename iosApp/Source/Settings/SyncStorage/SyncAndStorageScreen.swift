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
        autoDeletePeriod: .disabled
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
