//
//  FeedListView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import Reader
import SwiftUI

struct FeedListView: View {
    @Environment(\.openURL) private var openURL
    @Environment(HomeListIndexHolder.self) private var indexHolder
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(AppState.self) private var appState

    var loadingState: FeedUpdateStatus?
    var feedState: [FeedItem]
    var showLoading: Bool
    let currentFeedFilter: FeedFilter
    let columnVisibility: NavigationSplitViewVisibility
    let feedFontSizes: FeedFontSizes
    let swipeActions: SwipeActions

    let onReloadClick: () -> Void
    let onAddFeedClick: () -> Void
    let requestNewPage: () -> Void
    let onItemClick: (FeedItemUrlInfo) -> Void
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void
    let onBackToTimelineClick: () -> Void
    let onMarkAllAsReadClick: () -> Void
    let openDrawer: () -> Void

    var body: some View {
        if loadingState is NoFeedSourcesStatus {
            NoFeedsSourceView(onAddFeedClick: onAddFeedClick)
        } else if loadingState?.isLoading() == false && feedState.isEmpty {
            EmptyFeedView(
                currentFeedFilter: currentFeedFilter,
                onReloadClick: onReloadClick,
                onBackToTimelineClick: onBackToTimelineClick,
                openDrawer: openDrawer,
                columnVisibility: columnVisibility
            )
        } else if feedState.isEmpty {
            VStack(alignment: .center) {
                LoadingHeaderView(loadingState: loadingState, showLoading: showLoading)
                Spacer()
                ProgressView()
                Spacer()
            }
        } else {
            VStack(alignment: .center) {
                LoadingHeaderView(loadingState: loadingState, showLoading: showLoading)
                List {
                    ForEach(Array(feedState.enumerated()), id: \.element) { index, feedItem in
                        FeedItemRowView(
                            feedItem: feedItem,
                            index: index,
                            feedFontSizes: feedFontSizes,
                            swipeActions: swipeActions,
                            onItemClick: onItemClick,
                            onBookmarkClick: onBookmarkClick,
                            onReadStatusClick: onReadStatusClick
                        )
                        .onAppear {
                            if let index = feedState.firstIndex(of: feedItem) {
                                indexHolder.lastAppearedIndex = index
                                if index == feedState.count - 15 {
                                    requestNewPage()
                                }
                            }
                        }
                        .onDisappear {
                            if let index = feedState.firstIndex(of: feedItem) {
                                self.indexHolder.updateReadIndex(index: index)
                            }
                        }
                        if index == feedState.count - 1 {
                            if !(currentFeedFilter is FeedFilter.Read) {
                                Button(
                                    action: {
                                        onMarkAllAsReadClick()
                                    },
                                    label: {
                                        Text(feedFlowStrings.markAllReadButton)
                                    }
                                )
                                .buttonStyle(.borderless)
                                .frame(maxWidth: .infinity)
                                .listRowSeparator(.hidden)
                            }
                        }
                    }
                }
                .listStyle(PlainListStyle())
                .refreshable {
                    onReloadClick()
                }
            }
        }
    }
}

#Preview {
    FeedListView(
        loadingState: FinishedFeedUpdateStatus(),
        feedState: feedItemsForPreview,
        showLoading: false,
        currentFeedFilter: FeedFilter.Timeline(),
        columnVisibility: .all,
        feedFontSizes: defaultFeedFontSizes(),
        swipeActions: SwipeActions(leftSwipeAction: .none, rightSwipeAction: .none),
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {},
        onMarkAllAsReadClick: {},
        openDrawer: {}
    ).environment(HomeListIndexHolder(fakeHomeViewModel: true))
}

#Preview {
    FeedListView(
        loadingState: NoFeedSourcesStatus(),
        feedState: [],
        showLoading: false,
        currentFeedFilter: FeedFilter.Timeline(),
        columnVisibility: .all,
        feedFontSizes: defaultFeedFontSizes(),
        swipeActions: SwipeActions(leftSwipeAction: .none, rightSwipeAction: .none),
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {},
        onMarkAllAsReadClick: {},
        openDrawer: {}
    )
}

#Preview {
    FeedListView(
        loadingState: FinishedFeedUpdateStatus(),
        feedState: [],
        showLoading: false,
        currentFeedFilter: FeedFilter.Timeline(),
        columnVisibility: .all,
        feedFontSizes: defaultFeedFontSizes(),
        swipeActions: SwipeActions(leftSwipeAction: .none, rightSwipeAction: .none),
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {},
        onMarkAllAsReadClick: {},
        openDrawer: {}
    )
}
