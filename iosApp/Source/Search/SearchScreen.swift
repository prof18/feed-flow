import FeedFlowKit
import SwiftUI

struct SearchScreen: View {
  @Environment(AppState.self) private var appState

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
    .task {
      for await state in vmStoreOwner.instance.errorState {
        switch onEnum(of: state) {
        case .databaseError:
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.databaseError,
              subtitle: nil,
              showBanner: true
            )
          )

        case .feedErrorState(let state):
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.feedErrorMessage(state.feedName),
              subtitle: nil,
              showBanner: true
            )
          )

        case .syncError:
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.syncErrorMessage,
              subtitle: nil,
              showBanner: true
            )
          )
        }
      }
    }
  }
}

#Preview {
  SearchScreen()
}
