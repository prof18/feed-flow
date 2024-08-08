import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct SearchScreen: View {
    @EnvironmentObject private var appState: AppState

    @StateObject private var vmStoreOwner = VMStoreOwner<SearchViewModel>(KotlinDependencies.shared.getSearchViewModel())

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
            do {
                let stream = asyncSequence(for: vmStoreOwner.instance.searchQueryStateFlow)
                for try await state in stream {
                    self.searchText = state
                }
            } catch {
                if !(error is CancellationError) {
                                    self.appState.emitGenericError()
                                }            }
        }
        .task {
            do {
                let stream = asyncSequence(for: vmStoreOwner.instance.searchStateFlow)
                for try await state in stream {
                    self.searchState = state
                }
            } catch {
                if !(error is CancellationError) {
                                    self.appState.emitGenericError()
                                }            }
        }
    }
}

#Preview {
    SearchScreen()
}
