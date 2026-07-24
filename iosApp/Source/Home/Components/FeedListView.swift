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
    let isGridLayoutEnabled: Bool
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
    private let maxContentWidth: CGFloat = 720
    private let layoutWidthBucketSize: CGFloat = 8
    private let layoutWidthSettleDelay: Duration = .milliseconds(220)

    @State private var stableLayoutWidth: CGFloat = 0

    var body: some View {
        if loadingState is NoFeedSourcesStatus {
            NoFeedsSourceView(onAddFeedClick: onAddFeedClick)
        } else if loadingState?.isLoading() == false && feedState.isEmpty {
            EmptyFeedView(
                currentFeedFilter: currentFeedFilter,
                nextFeedPreviewState: nextFeedPreviewState,
                onReloadClick: onReloadClick,
                onBackToTimelineClick: onBackToTimelineClick,
                openDrawer: openDrawer,
                onNavigateToNextFeed: onNavigateToNextFeed,
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
                    let itemFeedLayout = normalizedLayout
                    let currentWidth = listProxy.size.width
                    let layoutDecisionWidth = stableLayoutWidth > 0 ? stableLayoutWidth : currentWidth
                    let isGridArrangement = isGridLayoutEnabled &&
                        itemFeedLayout.supportsGridArrangement &&
                        layoutDecisionWidth >= itemFeedLayout.gridMinContentWidth(maxContentWidth: maxContentWidth)
                    Group {
                        if isGridArrangement {
                            gridFeedContent(
                                feedItems: feedItems,
                                feedLayout: itemFeedLayout,
                                availableWidth: layoutDecisionWidth,
                                maxContentWidth: maxContentWidth
                            )
                            .onPreferenceChange(FeedItemVisibilityPreferenceKey.self) { items in
                                visibleItemsChanged(items: items, viewportHeight: listProxy.size.height)
                            }
                        } else {
                            List {
                                ForEach(feedItems, id: \.element.id) { index, feedItem in
                                    feedItemRow(
                                        index: index,
                                        feedItem: feedItem,
                                        feedLayout: itemFeedLayout,
                                        isGridCell: false,
                                        allowsSwipeActions: true
                                    )
                                    .listRowSeparator(itemFeedLayout == .list ? .automatic : .hidden)
                                    .listRowInsets(EdgeInsets())
                                    .listRowBackground(Color.clear)
                                }

                                footerButtons
                            }
                            .coordinateSpace(.named(coordinateSpaceName))
                            .listStyle(PlainListStyle())
                            .scrollContentBackground(.hidden)
                            .background(feedContentBackground(for: itemFeedLayout))
                            .onPreferenceChange(FeedItemVisibilityPreferenceKey.self) { items in
                                visibleItemsChanged(items: items, viewportHeight: listProxy.size.height)
                            }
                            .refreshable {
                                onReloadClick()
                            }
                            .contentMargins(
                                .horizontal,
                                max((currentWidth - maxContentWidth) / 2, 0),
                                for: .scrollContent
                            )
                        }
                    }
                    .transaction { transaction in
                        transaction.animation = nil
                    }
                    .task(id: layoutWidthBucket(for: currentWidth)) {
                        await updateStableLayoutWidth(currentWidth)
                    }
                }
            }
        }
    }

    private var normalizedLayout: FeedLayout {
        feedLayout == .grid ? .bigImage : feedLayout
    }

    private func gridFeedContent(
        feedItems: [(offset: Int, element: FeedItem)],
        feedLayout: FeedLayout,
        availableWidth: CGFloat,
        maxContentWidth: CGFloat
    ) -> some View {
        ScrollView {
            let columnCount = gridColumnCount(
                availableWidth: availableWidth,
                feedLayout: feedLayout,
                maxContentWidth: maxContentWidth
            )
            VStack(spacing: Spacing.regular) {
                HStack(alignment: .top, spacing: Spacing.regular) {
                    ForEach(0 ..< columnCount, id: \.self) { column in
                        LazyVStack(spacing: Spacing.regular) {
                            ForEach(
                                gridFeedItems(
                                    feedItems: feedItems,
                                    column: column,
                                    columnCount: columnCount
                                ),
                                id: \.element.id
                            ) { index, feedItem in
                                feedItemRow(
                                    index: index,
                                    feedItem: feedItem,
                                    feedLayout: feedLayout,
                                    isGridCell: true,
                                    allowsSwipeActions: false
                                )
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                }

                footerButtons
            }
        }
        .coordinateSpace(.named(coordinateSpaceName))
        .background(feedContentBackground(for: feedLayout))
        .safeAreaPadding(.horizontal, Spacing.regular)
        .safeAreaPadding(.top, Spacing.regular)
        .refreshable {
            onReloadClick()
        }
    }

    private func gridFeedItems(
        feedItems: [(offset: Int, element: FeedItem)],
        column: Int,
        columnCount: Int
    ) -> [(offset: Int, element: FeedItem)] {
        feedItems.enumerated().compactMap { visualIndex, feedItem in
            visualIndex % columnCount == column ? feedItem : nil
        }
    }

    private func gridColumnCount(
        availableWidth: CGFloat,
        feedLayout: FeedLayout,
        maxContentWidth: CGFloat
    ) -> Int {
        let contentWidth = max(availableWidth - Spacing.regular * 2, 0)
        let minCellWidth = feedLayout.gridMinCellWidth(maxContentWidth: maxContentWidth)
        return max(Int((contentWidth + Spacing.regular) / (minCellWidth + Spacing.regular)), 1)
    }

    private func layoutWidthBucket(for width: CGFloat) -> Int {
        Int((width / layoutWidthBucketSize).rounded(.toNearestOrAwayFromZero))
    }

    @MainActor
    private func updateStableLayoutWidth(_ width: CGFloat) async {
        if stableLayoutWidth == 0 {
            stableLayoutWidth = width
        }
        try? await Task.sleep(for: layoutWidthSettleDelay)
        guard !Task.isCancelled else {
            return
        }
        stableLayoutWidth = width
    }

    private func feedItemRow(
        index: Int,
        feedItem: FeedItem,
        feedLayout: FeedLayout,
        isGridCell: Bool,
        allowsSwipeActions: Bool
    ) -> some View {
        FeedItemRowView(
            feedItem: feedItem,
            index: index,
            feedFontSizes: feedFontSizes,
            swipeActions: swipeActions,
            feedLayout: feedLayout,
            isGridCell: isGridCell,
            currentFeedFilter: currentFeedFilter,
            allowsSwipeActions: allowsSwipeActions,
            feedItemDisplaySettings: feedItemDisplaySettings,
            onItemClick: onItemClick,
            onReaderModeClick: onReaderModeClick,
            onBookmarkClick: onBookmarkClick,
            onReadStatusClick: onReadStatusClick
        )
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
        }
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
            onScrollPositionChanged(index > 3)
        }
    }

    @ViewBuilder private var footerButtons: some View {
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

    private func visibleItemsChanged(items: [FeedItemVisibilityPreference], viewportHeight: CGFloat) {
        let visibleItems = items
            .filter { item in
                let visibleTop = max(item.minY, viewportVerticalInset)
                let visibleBottom = min(item.maxY, viewportHeight - viewportVerticalInset)
                return visibleBottom - visibleTop >= minimumVisibleRowHeight
            }
            .sorted { $0.minY < $1.minY }
            .map {
                VisibleFeedItem(id: $0.id, index: Int32($0.index))
            }
        indexHolder.visibleItemsChanged(visibleItems)
    }
}

private extension FeedLayout {
    var supportsGridArrangement: Bool {
        self == .card || self == .bigImage
    }

    var usesCardBackground: Bool {
        self == .card || self == .bigImage || self == .grid
    }

    func gridMinCellWidth(maxContentWidth: CGFloat) -> CGFloat {
        let cardWidth = (maxContentWidth - Spacing.regular) / 2
        if self == .bigImage {
            return cardWidth - (Spacing.medium + Spacing.small) - Spacing.medium - Spacing.regular
        }
        return cardWidth
    }

    func gridMinContentWidth(maxContentWidth: CGFloat) -> CGFloat {
        gridMinCellWidth(maxContentWidth: maxContentWidth) * 2 + Spacing.regular
    }
}

private func feedContentBackground(for layout: FeedLayout) -> Color {
    layout.usesCardBackground ? Color(.systemGroupedBackground) : Color(.systemBackground)
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
