//
//  HomeScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import KMPNativeCoroutinesAsync
import shared
import OrderedCollections

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

    @Binding
    var toggleListScroll: Bool

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
                openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItemClickedInfo.url))
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
            }
        )
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.loadingStateFlow)
                for try await state in stream {
                    let isLoading = state.isLoading() && state.totalFeedCount != 0
                    withAnimation {
                        self.showLoading = isLoading
                    }
                    self.indexHolder.isLoading = isLoading
                    self.loadingState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.errorState)
                for try await state in stream {
                    if let message = state?.message {
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: message.localized(),
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                    }
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.feedStateFlow)
                for try await state in stream {
                    self.feedState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.unreadCountFlow)
                for try await state in stream {
                    self.unreadCount = Int(truncating: state)
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.currentFeedFilterFlow)
                for try await state in stream {
                    self.currentFeedFilter = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .onChange(of: scenePhase) { newScenePhase in
            switch newScenePhase {
            case .background:
                homeViewModel.markAsReadOnScroll(lastVisibleIndex: Int32(indexHolder.getLastReadIndex()))
            default:
                break
            }
        }
    }
}
