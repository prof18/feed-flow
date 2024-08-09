import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct SearchScreen: View {
    @EnvironmentObject private var appState: AppState

    @StateObject
    private var vmStoreOwner = VMStoreOwner<SearchViewModel>(Deps.shared.getSearchViewModel())

    @State var searchText = ""

    @State var searchState: SearchState = SearchState.EmptyState()

    var body: some View {
        SearchScreenContent(
            searchText: $searchText,
            searchState: $searchState,
            onBookmarkClick: { (feedItemId, isBookmarked) in
                vmStoreOwner.instance.onBookmarkClick(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { (feedItemId, isRead) in
                vmStoreOwner.instance.onReadStatusClick(feedItemId: feedItemId, read: isRead)
            }
        ).onChange(of: searchText) { newValue in
            vmStoreOwner.instance.updateSearchQuery(query: newValue)
        }
        .task {
            for await state in vmStoreOwner.instance.searchQueryState {
                self.searchText = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.searchState {
                self.searchState = state
            }
        }
    }
}

#Preview {
    SearchScreen()
}
