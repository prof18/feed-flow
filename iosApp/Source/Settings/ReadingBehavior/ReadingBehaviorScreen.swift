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
        @Bindable var browserSelector = browserSelector

        ReadingBehaviorScreenContent(
            browserSelector: browserSelector,
            articleOpenMode: Binding(
                get: { settingsState.articleOpenMode },
                set: { vmStoreOwner.instance.updateArticleOpenMode(value: $0) }
            ),
            onArticleBrowserSelected: { browserId in
                vmStoreOwner.instance.updateArticleBrowser(browserId: browserId)
            },
            isMarkReadWhenScrollingEnabled: Binding(
                get: { settingsState.isMarkReadWhenScrollingEnabled },
                set: { vmStoreOwner.instance.updateMarkReadWhenScrolling(value: $0) }
            ),
            readArticlesVisibility: Binding(
                get: { settingsState.readArticlesVisibility },
                set: { vmStoreOwner.instance.updateReadArticlesVisibility(value: $0) }
            )
        )
        // Saving a new links browser can rewrite the stored article mode.
        .onChange(of: browserSelector.selectedBrowser) {
            vmStoreOwner.instance.refreshArticleOpenState()
        }
        .navigationTitle(Text(feedFlowStrings.settingsReadingBehavior))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            for await state in vmStoreOwner.instance.state {
                self.settingsState = state
            }
        }
    }
}
