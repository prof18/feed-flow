import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct SearchScreen: View {
    @EnvironmentObject private var appState: AppState

    @StateObject private var viewModel: SearchViewModel = KotlinDependencies.shared.getSearchViewModel()

    @State var searchText = ""

    @State var searchState: SearchState = SearchState.EmptyState()

    var body: some View {
        SearchScreenContent(
            searchText: $searchText,
            searchState: $searchState,
            onBookmarkClick: { (feedItemId, isBookmarked) in
               viewModel.onBookmarkClick(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { (feedItemId, isRead) in
                viewModel.onReadStatusClick(feedItemId: feedItemId, read: isRead)
            }
        ).onChange(of: searchText) { newValue in
            viewModel.updateSearchQuery(query: newValue)
        }
        .task {
            do {
                let stream = asyncSequence(for: viewModel.searchQueryStateFlow)
                for try await state in stream {
                    self.searchText = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: viewModel.searchStateFlow)
                for try await state in stream {
                    self.searchState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}

#Preview {
    SearchScreen()
}
