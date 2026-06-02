//
//  FeedListView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import Nuke
import NukeUI
import Reader
import SwiftUI

struct FeedListView: View {
    @Environment(\.openURL)
    private var openURL
    @Environment(HomeListIndexHolder.self)
    private var indexHolder
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(AppState.self)
    private var appState

    var loadingState: FeedUpdateStatus?
    var feedState: [FeedItem]
    var showLoading: Bool
    let currentFeedFilter: FeedFilter
    let columnVisibility: NavigationSplitViewVisibility
    let feedFontSizes: FeedFontSizes
    let swipeActions: SwipeActions
    let feedLayout: FeedLayout
    var feedItemDisplaySettings = FeedItemDisplaySettings(
        isHideUnreadDotEnabled: false,
        isHideFeedSourceEnabled: false,
        descriptionLineLimit: .three
    )
    let nextFeedPreviewState: NextFeedPreviewState

    let onReloadClick: () -> Void
    let onAddFeedClick: () -> Void
    let requestNewPage: () -> Void
    let onItemClick: (FeedItemUrlInfo) -> Void
    let onReaderModeClick: (FeedItemUrlInfo) -> Void
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void
    let onMarkAllAboveAsRead: (String) -> Void
    let onMarkAllBelowAsRead: (String) -> Void
    let onBackToTimelineClick: () -> Void
    let onNavigateToNextFeed: () -> Void
    let onMarkAllAsReadClick: () -> Void
    let openDrawer: () -> Void
    let onScrollPositionChanged: (Bool) -> Void
    let onOpenFeedSettings: (FeedSource) -> Void
    private let coordinateSpaceName = "FeedListViewCoordinateSpace"
    private let viewportVerticalInset: CGFloat = 8
    private let minimumVisibleRowHeight: CGFloat = 24

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
            let feedItems = Array(feedState.enumerated())
            VStack(alignment: .center) {
                LoadingHeaderView(loadingState: loadingState, showLoading: showLoading)
                GeometryReader { listProxy in
                    List {
                        ForEach(feedItems, id: \.element.id) { index, feedItem in
                            FeedItemRowView(
                                feedItem: feedItem,
                                index: index,
                                feedFontSizes: feedFontSizes,
                                swipeActions: swipeActions,
                                feedLayout: feedLayout,
                                currentFeedFilter: currentFeedFilter,
                                feedItemDisplaySettings: feedItemDisplaySettings,
                                onItemClick: onItemClick,
                                onReaderModeClick: onReaderModeClick,
                                onBookmarkClick: onBookmarkClick,
                                onReadStatusClick: onReadStatusClick
                            )
                            .if(feedLayout == .card) { view in
                                view
                                    .contextMenu {
                                        FeedItemContextMenu(
                                            feedItem: feedItem,
                                            onBookmarkClick: onBookmarkClick,
                                            onReadStatusClick: onReadStatusClick,
                                            onMarkAllAboveAsRead: onMarkAllAboveAsRead,
                                            onMarkAllBelowAsRead: onMarkAllBelowAsRead,
                                            onOpenFeedSettings: onOpenFeedSettings
                                        )
                                        .environment(browserSelector)
                                        .environment(appState)
                                    } preview: {
                                        FeedItemView(
                                            feedItem: feedItem,
                                            index: index,
                                            feedFontSizes: feedFontSizes,
                                            feedLayout: feedLayout,
                                            currentFeedFilter: currentFeedFilter,
                                            feedItemDisplaySettings: feedItemDisplaySettings
                                        )
                                        .fixedSize(horizontal: false, vertical: true)
                                        .frame(width: 350)
                                        .background(Color(.secondarySystemBackground))
                                    }
                            }
                            .if(feedLayout == .list) { view in
                                view
                                    .contextMenu {
                                        FeedItemContextMenu(
                                            feedItem: feedItem,
                                            onBookmarkClick: onBookmarkClick,
                                            onReadStatusClick: onReadStatusClick,
                                            onMarkAllAboveAsRead: onMarkAllAboveAsRead,
                                            onMarkAllBelowAsRead: onMarkAllBelowAsRead,
                                            onOpenFeedSettings: onOpenFeedSettings
                                        )
                                        .environment(browserSelector)
                                        .environment(appState)
                                    } preview: {
                                        FeedItemView(
                                            feedItem: feedItem,
                                            index: index,
                                            feedFontSizes: feedFontSizes,
                                            feedLayout: feedLayout,
                                            currentFeedFilter: currentFeedFilter
                                        )
                                        .frame(width: 350, alignment: .leading)
                                    }
                            }
                            .listRowSeparator(feedLayout == .card ? .hidden : .automatic)
                            .listRowInsets(EdgeInsets())
                            .background(
                                FeedItemVisibilityReader(
                                    id: feedItem.id,
                                    index: index,
                                    coordinateSpaceName: coordinateSpaceName
                                )
                            )
                            .onAppear {
                                if index == feedState.count - 15 {
                                    requestNewPage()
                                }
                                // Show scroll to top button when user has scrolled past first 3 items
                                onScrollPositionChanged(index > 3)
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

                                if let enabledState = nextFeedPreviewState
                                    as? NextFeedPreviewState.NextFeedPreviewEnabledState {
                                    NextFeedButton(
                                        title: enabledState.title,
                                        onNavigateNext: onNavigateToNextFeed
                                    )
                                }
                            }
                        }
                    }
                    .coordinateSpace(name: coordinateSpaceName)
                    .listStyle(PlainListStyle())
                    .onPreferenceChange(FeedItemVisibilityPreferenceKey.self) { items in
                        let visibleItems = items
                            .filter { item in
                                let visibleTop = max(item.minY, viewportVerticalInset)
                                let visibleBottom = min(item.maxY, listProxy.size.height - viewportVerticalInset)
                                return visibleBottom - visibleTop >= minimumVisibleRowHeight
                            }
                            .sorted { $0.minY < $1.minY }
                            .map {
                                VisibleFeedItem(id: $0.id, index: Int32($0.index))
                            }
                        indexHolder.visibleItemsChanged(visibleItems)
                    }
                    .refreshable {
                        onReloadClick()
                    }
                }
            }
        }
    }
}

private struct FeedItemVisibilityPreference: Equatable {
    let id: String
    let index: Int
    let minY: CGFloat
    let maxY: CGFloat
}

private struct FeedItemVisibilityPreferenceKey: PreferenceKey {
    static let defaultValue: [FeedItemVisibilityPreference] = []

    static func reduce(
        value: inout [FeedItemVisibilityPreference],
        nextValue: () -> [FeedItemVisibilityPreference]
    ) {
        value.append(contentsOf: nextValue())
    }
}

private struct FeedItemVisibilityReader: View {
    let id: String
    let index: Int
    let coordinateSpaceName: String

    var body: some View {
        GeometryReader { proxy in
            let frame = proxy.frame(in: .named(coordinateSpaceName))
            Color.clear.preference(
                key: FeedItemVisibilityPreferenceKey.self,
                value: [
                    FeedItemVisibilityPreference(
                        id: id,
                        index: index,
                        minY: frame.minY,
                        maxY: frame.maxY
                    )
                ]
            )
        }
    }
}
