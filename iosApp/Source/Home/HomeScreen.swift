//
//  HomeScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import FeedFlowKit

struct HomeScreen: View {

    @EnvironmentObject
    private var appState: AppState

    @EnvironmentObject
    private var browserSelector: BrowserSelector

    @EnvironmentObject
    private var indexHolder: HomeListIndexHolder

    @Environment(\.scenePhase)
    private var scenePhase

    @Environment(\.openURL)
    private var openURL

    @State
    var loadingState: FeedUpdateStatus?

    @State
    var feedState: [FeedItem] = []

    @State
    var showLoading: Bool = true

    @State
    private var sheetToShow: HomeSheetToShow?

    @State
    var unreadCount = 0

    @State
    var currentFeedFilter: FeedFilter = FeedFilter.Timeline()

    @State
    var showFeedSyncButton: Bool = false

    @Binding
    var toggleListScroll: Bool

    @Binding
    var showSettings: Bool

    @Binding var selectedDrawerItem: DrawerItem?

    let homeViewModel: HomeViewModel

    var body: some View {
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
            onBookmarkClick: { feedItemId, isBookmarked in
                homeViewModel.updateBookmarkStatus(feedItemId: feedItemId, bookmarked: isBookmarked)
            },
            onReadStatusClick: { feedItemId, isRead in
                homeViewModel.updateReadStatus(feedItemId: feedItemId, read: isRead)
            },
            onBackToTimelineClick: {
                homeViewModel.onFeedFilterSelected(selectedFeedFilter: FeedFilter.Timeline())
                selectedDrawerItem = DrawerItem.Timeline()
            },
            onFeedSyncClick: {
                homeViewModel.enqueueBackup()
            }
        )
        .task {
            for await state in homeViewModel.loadingState {
                let isLoading = state.isLoading() && state.totalFeedCount != 0
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
                case .none:
                    break
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
        .onChange(of: scenePhase) { newScenePhase in
            switch newScenePhase {
            case .background:
                homeViewModel.enqueueBackup()
            default:
                break
            }
        }
    }
}
