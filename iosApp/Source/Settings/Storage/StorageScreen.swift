import FeedFlowKit
import SwiftUI

struct StorageScreen: View {
    @StateObject private var vmStoreOwner = VMStoreOwner<SyncAndStorageSettingsViewModel>(
        Deps.shared.getSyncAndStorageSettingsViewModel()
    )
    @StateObject private var readingVmStoreOwner = VMStoreOwner<ReadingBehaviorSettingsViewModel>(
        Deps.shared.getReadingBehaviorSettingsViewModel()
    )
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = SyncAndStorageState(
        syncPeriod: .oneHour,
        backgroundSyncRestrictions: BackgroundSyncRestrictions(syncOnlyOnWifi: false, syncOnlyWhenCharging: false),
        autoDeletePeriod: .disabled,
        refreshFeedsOnLaunch: true
    )

    @State private var readingState = ReadingBehaviorState(
        isSaveReaderModeContentEnabled: false,
        isPrefetchArticleContentEnabled: false,
        isMarkReadWhenScrollingEnabled: false,
        isShowReadItemsEnabled: false,
        isHideReadItemsEnabled: false,
        readArticlesVisibility: .hideOnRefresh,
        articleOpenMode: .fullArticle,
        articleBrowserId: nil
    )

    var body: some View {
        StorageScreenContent(
            autoDeletePeriod: Binding(
                get: { settingsState.autoDeletePeriod },
                set: { vmStoreOwner.instance.updateAutoDeletePeriod(period: $0) }
            ),
            isSaveReaderModeContentEnabled: Binding(
                get: { readingState.isSaveReaderModeContentEnabled },
                set: { readingVmStoreOwner.instance.updateSaveReaderModeContent(value: $0) }
            ),
            isPrefetchArticleContentEnabled: Binding(
                get: { readingState.isPrefetchArticleContentEnabled },
                set: { readingVmStoreOwner.instance.updatePrefetchArticleContent(value: $0) }
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
        .task {
            for await state in readingVmStoreOwner.instance.state {
                self.readingState = state
            }
        }
    }
}
