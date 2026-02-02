import FeedFlowKit
import SwiftUI

struct ReadingBehaviorScreen: View {
    @Environment(AppState.self) 
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<ReadingBehaviorSettingsViewModel>(
        Deps.shared.getReadingBehaviorSettingsViewModel()
    )
    @State private var browserSelector = BrowserSelector()
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = ReadingBehaviorState(
        isReaderModeEnabled: false,
        isSaveReaderModeContentEnabled: false,
        isPrefetchArticleContentEnabled: false,
        isMarkReadWhenScrollingEnabled: false,
        isShowReadItemsEnabled: false,
        isHideReadItemsEnabled: false
    )

    var body: some View {
        @Bindable var appState = appState
        @Bindable var browserSelector = browserSelector

        ReadingBehaviorScreenContent(
            browserSelector: browserSelector,
            isReaderModeEnabled: Binding(
                get: { settingsState.isReaderModeEnabled },
                set: { vmStoreOwner.instance.updateReaderMode(value: $0) }
            ),
            isSaveReaderModeContentEnabled: Binding(
                get: { settingsState.isSaveReaderModeContentEnabled },
                set: { vmStoreOwner.instance.updateSaveReaderModeContent(value: $0) }
            ),
            isPrefetchArticleContentEnabled: Binding(
                get: { settingsState.isPrefetchArticleContentEnabled },
                set: { vmStoreOwner.instance.updatePrefetchArticleContent(value: $0) }
            ),
            isMarkReadWhenScrollingEnabled: Binding(
                get: { settingsState.isMarkReadWhenScrollingEnabled },
                set: { vmStoreOwner.instance.updateMarkReadWhenScrolling(value: $0) }
            ),
            isShowReadItemEnabled: Binding(
                get: { settingsState.isShowReadItemsEnabled },
                set: { vmStoreOwner.instance.updateShowReadItemsOnTimeline(value: $0) }
            ),
            isHideReadItemsEnabled: Binding(
                get: { settingsState.isHideReadItemsEnabled },
                set: { vmStoreOwner.instance.updateHideReadItems(value: $0) }
            )
        )
        .navigationTitle(Text(feedFlowStrings.settingsReadingBehavior))
        .navigationBarTitleDisplayMode(.inline)
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in vmStoreOwner.instance.state {
                self.settingsState = state
            }
        }
    }
}
