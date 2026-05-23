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
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(HomeListIndexHolder.self)
    private var indexHolder
    @Environment(\.scenePhase)
    private var scenePhase
    @Environment(\.openURL)
    private var openURL

    @State var loadingState: FeedUpdateStatus?

    @State var feedState: [FeedItem] = []

    @State var showLoading = true

    @State private var sheetToShow: HomeSheetToShow?

    @State var unreadCount = 0

    @State var isUnreadCountHidden = false

    @State var currentFeedFilter: FeedFilter = .Timeline()

    @State var showFeedSyncButton = false

    @State var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()

    @State var swipeActions: SwipeActions = .init(leftSwipeAction: .none, rightSwipeAction: .none)

    @State var feedLayout: FeedLayout = .list

    @State var nextFeedPreviewState: NextFeedPreviewState = .NextFeedPreviewDisabledState()

    @State var feedItemDisplaySettings = FeedItemDisplaySettings(
        isHideUnreadDotEnabled: false,
        isHideFeedSourceEnabled: false,
        descriptionLineLimit: .three
    )

    @State var viewMenuState = HomeViewMenuState(
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
            loadingState: $loadingState,
            feedState: $feedState,
            showLoading: $showLoading,
            unreadCount: $unreadCount,
            isUnreadCountHidden: $isUnreadCountHidden,
            sheetToShow: $sheetToShow,
            toggleListScroll: $toggleListScroll,
            currentFeedFilter: $currentFeedFilter,
            showSettings: $showSettings,
            showFeedSyncButton: $showFeedSyncButton,
            columnVisibility: $columnVisibility,
            feedFontSizes: $feedFontSizes,
            swipeActions: $swipeActions,
            feedLayout: $feedLayout,
            nextFeedPreviewState: $nextFeedPreviewState,
            feedItemDisplaySettings: $feedItemDisplaySettings,
            viewMenuState: $viewMenuState,
            onRefresh: {
                homeViewModel.getNewFeeds(isFirstLaunch: false)
            },
            onMarkAllReadClick: {
                homeViewModel.markAllRead()
            },
            onDeleteOldFeedClick: {
                homeViewModel.deleteOldFeedItems()
            },
            onForceRefreshClick: {
                homeViewModel.forceFeedRefresh()
            },
            deleteAllFeeds: {
                homeViewModel.deleteAllFeeds()
            },
            requestNewPage: {
                homeViewModel.requestNewFeedsPage()
            },
            onItemClick: { feedItemClickedInfo in
                homeViewModel.markAsRead(feedItemId: feedItemClickedInfo.id)
            },
            onReaderModeClick: { feedItemUrlInfo in
                readerModeViewModel.getReaderModeHtml(urlInfo: feedItemUrlInfo)
                if let navigate = onReaderModeNavigate {
                    navigate()
                } else {
                    appState.navigate(route: CommonViewRoute.readerMode)
                }
            },
            onBookmarkClick: { feedItemId, isBookmarked in
                homeViewModel.updateBookmarkStatus(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { feedItemId, isRead in
                homeViewModel.updateReadStatus(feedItemId: feedItemId, read: isRead)
            },
            onMarkAllAboveAsRead: { feedItemId in
                homeViewModel.markAllAboveAsRead(feedItemId: feedItemId)
            },
            onMarkAllBelowAsRead: { feedItemId in
                homeViewModel.markAllBelowAsRead(feedItemId: feedItemId)
            },
            onBackToTimelineClick: {
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
            onFeedSyncClick: {
                homeViewModel.enqueueBackup()
            },
            openDrawer: openDrawer,
            onFeedOrderChange: { order in
                homeViewModel.updateFeedOrder(order: order)
            },
            onShowReadArticlesTimelineChange: { value in
                homeViewModel.updateShowReadArticlesTimeline(value: value)
            }
        )
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in homeViewModel.loadingState {
                let isLoading = state.isLoading()
                withAnimation {
                    self.showLoading = isLoading
                }
                if isLoading {
                    indexHolder.pauseUpdates()
                } else {
                    indexHolder.resumeUpdates()
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
}
