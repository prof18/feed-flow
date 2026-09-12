//
//  HomeScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct HomeScreen: View {
    @Environment(AppState.self)
    private var appState
    @Environment(HomeListIndexHolder.self)
    private var indexHolder
    @Environment(\.scenePhase)
    private var scenePhase

    @State private var loadingState: FeedUpdateStatus?

    @State private var feedState: [FeedItem] = []

    @State private var showLoading = true

    @State private var sheetToShow: HomeSheetToShow?

    @State private var unreadCount = 0

    @State private var isUnreadCountHidden = false

    @State private var currentFeedFilter: FeedFilter = .Timeline()

    @State private var showFeedSyncButton = false

    @State private var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()

    @State private var swipeActions: SwipeActions = .init(leftSwipeAction: .none, rightSwipeAction: .none)

    @State private var feedLayout: FeedLayout = .list
    @State private var isGridLayoutEnabled = false

    @State private var nextFeedPreviewState: NextFeedPreviewState = .NextFeedPreviewDisabledState()

    @State private var feedItemDisplaySettings = FeedItemDisplaySettings(
        isHideUnreadDotEnabled: false,
        isHideFeedSourceEnabled: false,
        descriptionLineLimit: .three
    )

    @State private var viewMenuState = HomeViewMenuState(
        feedOrder: .newestFirst,
        showReadArticlesTimeline: false
    )

    @Binding var toggleListScroll: Bool

    @Binding var showSettings: Bool

    @Binding var columnVisibility: NavigationSplitViewVisibility

    let homeViewModel: HomeViewModel
    let readerModeViewModel: ReaderModeViewModel
    let onReaderModeNavigate: (() -> Void)?

    let openDrawer: () -> Void
    var onSidebarSelectionChanged: ((FeedFilter) -> Void)?

    var body: some View {
        @Bindable var appState = appState

        HomeContent(
            loadingState: loadingState,
            feedState: feedState,
            showLoading: showLoading,
            unreadCount: unreadCount,
            isUnreadCountHidden: isUnreadCountHidden,
            sheetToShow: $sheetToShow,
            toggleListScroll: toggleListScroll,
            currentFeedFilter: currentFeedFilter,
            showSettings: showSettings,
            showFeedSyncButton: showFeedSyncButton,
            columnVisibility: columnVisibility,
            feedFontSizes: feedFontSizes,
            swipeActions: swipeActions,
            feedLayout: feedLayout,
            isGridLayoutEnabled: isGridLayoutEnabled,
            nextFeedPreviewState: nextFeedPreviewState,
            feedItemDisplaySettings: feedItemDisplaySettings,
            viewMenuState: viewMenuState,
            onRefresh: { [homeViewModel] in
                homeViewModel.refreshCurrentFilter()
            },
            onMarkAllReadClick: { [homeViewModel] in
                homeViewModel.markAllRead()
            },
            onDeleteOldFeedClick: { [homeViewModel] in
                homeViewModel.deleteOldFeedItems()
            },
            deleteAllFeeds: { [homeViewModel] in
                homeViewModel.deleteAllFeeds()
            },
            requestNewPage: { [homeViewModel] in
                homeViewModel.requestNewFeedsPage()
            },
            onItemClick: { [homeViewModel] feedItemClickedInfo in
                homeViewModel.markAsRead(feedItemId: feedItemClickedInfo.id)
            },
            onReaderModeClick: onReaderModeClick,
            onBookmarkClick: { [homeViewModel] feedItemId, isBookmarked in
                homeViewModel.updateBookmarkStatus(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { [homeViewModel] feedItemId, isRead in
                homeViewModel.updateReadStatus(feedItemId: feedItemId, read: isRead)
            },
            onMarkAllAboveAsRead: { [homeViewModel] feedItemId in
                homeViewModel.markAllAboveAsRead(feedItemId: feedItemId)
            },
            onMarkAllBelowAsRead: { [homeViewModel] feedItemId in
                homeViewModel.markAllBelowAsRead(feedItemId: feedItemId)
            },
            onBackToTimelineClick: { [homeViewModel] in
                homeViewModel.onFeedFilterSelected(selectedFeedFilter: FeedFilter.Timeline())
            },
            onNavigateToNextFeed: {
                toggleListScroll.toggle()
                if let nextFeed = homeViewModel.nextFeedPreviewState.value
                    as? NextFeedPreviewState.NextFeedPreviewEnabledState {
                    onSidebarSelectionChanged?(nextFeed.feedFilter)
                }
                homeViewModel.onNavigateToNextFeed()
            },
            onFeedSyncClick: { [homeViewModel] in
                homeViewModel.enqueueBackup()
            },
            openDrawer: openDrawer,
            onFeedOrderChange: { [homeViewModel] order in
                homeViewModel.updateFeedOrder(order: order)
            },
            onShowReadArticlesTimelineChange: { [homeViewModel] value in
                homeViewModel.updateShowReadArticlesTimeline(value: value)
            },
            onSettingsDone: { [homeViewModel] in
                homeViewModel.reloadFeedState()
            }
        )
        .refreshable { [homeViewModel] in
            homeViewModel.refreshCurrentFilter()
        }
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in homeViewModel.loadingState {
                let isLoading = state.isLoading()
                withAnimation {
                    self.showLoading = isLoading
                }
                self.loadingState = state
            }
        }
        .task {
            for await state in homeViewModel.errorState {
                switch onEnum(of: state) {
                case let .databaseError(errorState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.databaseError(errorState.errorCode.code),
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
        .task {
            for await state in homeViewModel.feedState {
                self.feedState = state
            }
        }
        .task {
            for await state in homeViewModel.unreadCountFlow {
                self.unreadCount = Int(truncating: state)
            }
        }
        .task {
            for await state in homeViewModel.isUnreadCountHidden {
                self.isUnreadCountHidden = state as? Bool ?? false
            }
        }
        .task {
            for await state in homeViewModel.currentFeedFilter {
                self.currentFeedFilter = state
            }
        }
        .task {
            for await state in homeViewModel.isSyncUploadRequired {
                self.showFeedSyncButton = state as? Bool ?? false
            }
        }
        .task {
            for await state in homeViewModel.feedFontSizeState {
                self.feedFontSizes = state
            }
        }
        .task {
            for await state in homeViewModel.swipeActions {
                self.swipeActions = state
            }
        }
        .task {
            for await state in homeViewModel.feedLayout {
                self.feedLayout = state
            }
        }
        .task {
            for await state in homeViewModel.isGridLayoutEnabled {
                self.isGridLayoutEnabled = state as? Bool ?? false
            }
        }
        .task {
            for await state in homeViewModel.nextFeedPreviewState {
                self.nextFeedPreviewState = state
            }
        }
        .task {
            for await state in homeViewModel.feedItemDisplaySettings {
                self.feedItemDisplaySettings = state
            }
        }
        .task {
            for await state in homeViewModel.viewMenuState {
                self.viewMenuState = state
            }
        }
        .onChange(of: scenePhase) {
            switch scenePhase {
            case .background:
                homeViewModel.enqueueBackup()
            default:
                break
            }
        }
    }

    private var onReaderModeClick: (FeedItemUrlInfo) -> Void {
        if let navigate = onReaderModeNavigate {
            return { [homeViewModel, readerModeViewModel, navigate] urlInfo in
                homeViewModel.markAsRead(feedItemId: urlInfo.id)
                readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                navigate()
            }
        }

        return { [homeViewModel, readerModeViewModel, appState] urlInfo in
            homeViewModel.markAsRead(feedItemId: urlInfo.id)
            readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
            appState.navigate(route: CommonViewRoute.readerMode)
        }
    }
}
