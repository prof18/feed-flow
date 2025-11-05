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

    @State var currentFeedFilter: FeedFilter = .Timeline()

    @State var showFeedSyncButton = false

    @State var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()

    @State var showFeedOperationDialog = false

    @State var feedOperationLoadingMessage: String?

    @State var swipeActions: SwipeActions = .init(leftSwipeAction: .none, rightSwipeAction: .none)

    @State var feedLayout: FeedLayout = .list

    @Binding var toggleListScroll: Bool

    @Binding var showSettings: Bool

    @Binding var selectedDrawerItem: DrawerItem?

    @Binding var columnVisibility: NavigationSplitViewVisibility

    let homeViewModel: HomeViewModel
    let readerModeViewModel: ReaderModeViewModel

    let openDrawer: () -> Void

    var body: some View {
        @Bindable var appState = appState

        HomeContent(
            loadingState: $loadingState,
            feedState: $feedState,
            showLoading: $showLoading,
            unreadCount: $unreadCount,
            sheetToShow: $sheetToShow,
            toggleListScroll: $toggleListScroll,
            currentFeedFilter: $currentFeedFilter,
            showSettings: $showSettings,
            showFeedSyncButton: $showFeedSyncButton,
            columnVisibility: $columnVisibility,
            feedFontSizes: $feedFontSizes,
            swipeActions: $swipeActions,
            feedLayout: $feedLayout,
            onRefresh: {
                homeViewModel.getNewFeeds(isFirstLaunch: false)
            },
            updateReadStatus: { index in
                homeViewModel.markAsReadOnScroll(lastVisibleIndex: index)
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
                appState.navigate(route: CommonViewRoute.readerMode)
            },
            onBookmarkClick: { feedItemId, isBookmarked in
                homeViewModel.updateBookmarkStatus(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { feedItemId, isRead in
                homeViewModel.updateReadStatus(feedItemId: feedItemId, read: isRead)
            },
            onBackToTimelineClick: {
                homeViewModel.onFeedFilterSelected(selectedFeedFilter: FeedFilter.Timeline())
            },
            onFeedSyncClick: {
                homeViewModel.enqueueBackup()
            },
            openDrawer: openDrawer
        )
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in homeViewModel.loadingState {
                let isLoading = state.isLoading()
                withAnimation {
                    self.showLoading = isLoading
                }
                self.indexHolder.isLoading = isLoading
                self.loadingState = state
            }
        }
        .task {
            for await state in homeViewModel.errorState {
                switch onEnum(of: state) {
                case .databaseError:
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.databaseError(state.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                case let .feedErrorState(state):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.feedErrorMessage(state.feedName, state.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                case .syncError:
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.syncErrorMessage(state.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            }
        }
        .loadingDialog(isLoading: showFeedOperationDialog, message: feedOperationLoadingMessage)
        .task {
            for await state in homeViewModel.feedOperationState {
                switch onEnum(of: state) {
                case .none:
                    self.feedOperationLoadingMessage = nil
                    self.showFeedOperationDialog = false
                case .deleting:
                    self.feedOperationLoadingMessage = feedFlowStrings.deletingFeedDialogTitle
                    self.showFeedOperationDialog = true
                case .markingAllRead:
                    self.feedOperationLoadingMessage = feedFlowStrings.markingAllReadDialogTitle
                    self.showFeedOperationDialog = true
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
