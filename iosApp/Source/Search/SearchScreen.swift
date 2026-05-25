import FeedFlowKit
import SwiftUI

struct SearchScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<SearchViewModel>(Deps.shared.getSearchViewModel())

    @State var searchText = ""

    @State var searchState: SearchState = .EmptyState()

    @State var searchFilter: SearchFilter = .all

    @State var currentFeedFilter: FeedFilter?

    @State var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()

    @State var feedItemDisplaySettings = FeedItemDisplaySettings(
        isHideUnreadDotEnabled: false,
        isHideFeedSourceEnabled: false,
        descriptionLineLimit: .three
    )

    private let initialSearchText: String?
    private let initialSearchFilterKey: String?
    let readerModeViewModel: ReaderModeViewModel
    let onReaderModeNavigate: (() -> Void)?

    init(
        initialSearchText: String? = nil,
        initialSearchFilter: String? = nil,
        readerModeViewModel: ReaderModeViewModel,
        onReaderModeNavigate: (() -> Void)?
    ) {
        self.initialSearchText = initialSearchText
        initialSearchFilterKey = initialSearchFilter
        self.readerModeViewModel = readerModeViewModel
        self.onReaderModeNavigate = onReaderModeNavigate
        _searchText = State(initialValue: initialSearchText ?? "")
        _searchFilter = State(initialValue: Self.searchFilter(from: initialSearchFilter) ?? .all)
    }

    var body: some View {
        @Bindable var appState = appState
        
        SearchScreenContent(
            searchText: $searchText,
            searchState: $searchState,
            searchFilter: $searchFilter,
            currentFeedFilter: currentFeedFilter,
            feedFontSizes: $feedFontSizes,
            feedItemDisplaySettings: feedItemDisplaySettings,
            readerModeViewModel: readerModeViewModel,
            onReaderModeNavigate: onReaderModeNavigate,
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
            var shouldIgnoreFirstEmptyQuery = false
            if let initialSearchText, !initialSearchText.isEmpty {
                self.searchText = initialSearchText
                vmStoreOwner.instance.updateSearchQuery(query: initialSearchText)
                shouldIgnoreFirstEmptyQuery = true
            }
            for await state in vmStoreOwner.instance.searchQueryState {
                if shouldIgnoreFirstEmptyQuery, state.isEmpty {
                    shouldIgnoreFirstEmptyQuery = false
                    continue
                }
                shouldIgnoreFirstEmptyQuery = false
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
            var shouldIgnoreFirstDefaultFilter = false
            if let initialSearchFilter = Self.searchFilter(from: initialSearchFilterKey) {
                self.searchFilter = initialSearchFilter
                vmStoreOwner.instance.updateSearchFilter(filter: initialSearchFilter)
                shouldIgnoreFirstDefaultFilter = true
            }
            for await state in vmStoreOwner.instance.searchFilterState {
                if shouldIgnoreFirstDefaultFilter, state == .all {
                    shouldIgnoreFirstDefaultFilter = false
                    continue
                }
                shouldIgnoreFirstDefaultFilter = false
                self.searchFilter = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.searchFeedFilterState {
                self.currentFeedFilter = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.feedItemDisplaySettings {
                self.feedItemDisplaySettings = state
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

    private static func searchFilter(from key: String?) -> SearchFilter? {
        switch key {
        case "read":
            return .read
        case "bookmarks":
            return .bookmarks
        default:
            return nil
        }
    }
}
