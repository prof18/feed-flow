import SwiftUI
import FeedFlowKit

struct SearchScreen: View {
    @StateObject
    private var vmStoreOwner = VMStoreOwner<SearchViewModel>(Deps.shared.getSearchViewModel())

    @State var searchText = ""

    @State var searchState: SearchState = SearchState.EmptyState()

    @State var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()

    var body: some View {
        SearchScreenContent(
            searchText: $searchText,
            searchState: $searchState,
            feedFontSizes: $feedFontSizes,
            onBookmarkClick: { (feedItemId, isBookmarked) in
                vmStoreOwner.instance.onBookmarkClick(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { (feedItemId, isRead) in
                vmStoreOwner.instance.onReadStatusClick(feedItemId: feedItemId, read: isRead)
            }
        ).onChange(of: searchText) {
            vmStoreOwner.instance.updateSearchQuery(query: searchText)
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
        .task {
            for await state in vmStoreOwner.instance.feedFontSizeState {
                self.feedFontSizes = state
            }
        }
    }
}

#Preview {
    SearchScreen()
}
