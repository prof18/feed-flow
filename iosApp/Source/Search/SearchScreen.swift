import FeedFlowKit
import SwiftUI

struct SearchScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<SearchViewModel>(Deps.shared.getSearchViewModel())

    @State var searchText = ""

    @State var searchState: SearchState = .EmptyState()

    @State var searchFilter: SearchFilter = .timeline

    @State var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()

    let readerModeViewModel: ReaderModeViewModel
    
    var body: some View {
        @Bindable var appState = appState
        
        SearchScreenContent(
            searchText: $searchText,
            searchState: $searchState,
            searchFilter: $searchFilter,
            feedFontSizes: $feedFontSizes,
            readerModeViewModel: readerModeViewModel,
            onSearchFilterSelected: { filter in
                vmStoreOwner.instance.updateSearchFilter(filter: filter)
            },
            onBookmarkClick: { feedItemId, isBookmarked in
                vmStoreOwner.instance.onBookmarkClick(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { feedItemId, isRead in
                vmStoreOwner.instance.onReadStatusClick(feedItemId: feedItemId, read: isRead)
            },
            onMarkAllAboveAsRead: { feedItemId in
                vmStoreOwner.instance.markAllAboveAsRead(targetItemId: feedItemId)
            },
            onMarkAllBelowAsRead: { feedItemId in
                vmStoreOwner.instance.markAllBelowAsRead(targetItemId: feedItemId)
            }
        )
        .snackbar(messageQueue: $appState.snackbarQueue)
        .onChange(of: searchText) {
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
            for await state in vmStoreOwner.instance.searchFilterState {
                self.searchFilter = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.errorState {
                switch onEnum(of: state) {
                case let .databaseError(errorState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.databaseError(errorState.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                    
                case let .feedErrorState(feedError):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.feedErrorMessageImproved(feedError.feedName),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                    
                case let .syncError(errorState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.syncErrorMessage(errorState.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                    
                case .deleteFeedSourceError:
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.deleteFeedSourceError,
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            }
        }
    }
}
