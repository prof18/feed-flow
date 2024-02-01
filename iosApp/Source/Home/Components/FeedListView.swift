//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import OrderedCollections

struct FeedListView: View {
    @Environment(\.openURL) private var openURL

    @EnvironmentObject private var indexHolder: HomeListIndexHolder
    @EnvironmentObject private var browserSelector: BrowserSelector

    var loadingState: FeedUpdateStatus?
    var feedState: [FeedItem]
    var showLoading: Bool
    let currentFeedFilter: FeedFilter

    let onReloadClick: () -> Void
    let onAddFeedClick: () -> Void
    let requestNewPage: () -> Void
    let onItemClick: (FeedItemUrlInfo) -> Void
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void

    var body: some View {
        if loadingState is NoFeedSourcesStatus {
            NoFeedsSourceView(onAddFeedClick: onAddFeedClick)
        } else if loadingState?.isLoading() == false && feedState.isEmpty {
            EmptyFeedView(
                currentFeedFilter: currentFeedFilter,
                onReloadClick: onReloadClick
            )
        } else if feedState.isEmpty {
            VStack(alignment: .center) {
                loadingHeader
                Spacer()
                ProgressView()
                Spacer()
            }
        } else {
            VStack(alignment: .center) {
                loadingHeader
                List {
                    ForEach(Array(feedState.enumerated()), id: \.element) { index, feedItem in
                        FeedItemView(feedItem: feedItem, index: index)
                            .id(feedItem.id)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                onItemClick(FeedItemUrlInfo(id: feedItem.id, url: feedItem.url))
                            }
                            .contextMenu {
                                VStack {
                                    makeReadUnreadButton(feedItem: feedItem)
                                    makeBookmarkButton(feedItem: feedItem)
                                    makeCommentsButton(feedItem: feedItem)
                                }
                            }
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
                    }
                }
                .listStyle(PlainListStyle())
                .refreshable {
                    onReloadClick()
                }
            }
        }
    }

    @ViewBuilder
    private func makeReadUnreadButton(feedItem: FeedItem) -> some View {
        Button {
            onReadStatusClick(FeedItemId(id: feedItem.id), !feedItem.isRead)
        } label: {
            if feedItem.isRead {
                Label(localizer.menu_mark_as_unread.localized, systemImage: "envelope.badge")
            } else {
                Label(localizer.menu_mark_as_read.localized, systemImage: "envelope.open")
            }
        }
    }

    @ViewBuilder
    private func makeBookmarkButton(feedItem: FeedItem) -> some View {
        Button {
            onBookmarkClick(FeedItemId(id: feedItem.id), !feedItem.isBookmarked)
        } label: {
            if feedItem.isBookmarked {
                Label(localizer.menu_remove_from_bookmark.localized, systemImage: "bookmark.slash")
            } else {
                Label(localizer.menu_add_to_bookmark.localized, systemImage: "bookmark")
            }
        }
    }

    @ViewBuilder
    private func makeCommentsButton(feedItem: FeedItem) -> some View {
        if let commentsUrl = feedItem.commentsUrl {
            Button {
                openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: commentsUrl))
            } label: {
                Label(localizer.menu_open_comments.localized, systemImage: "bubble.left.and.bubble.right")
            }
        }
    }

    @ViewBuilder
    private var loadingHeader: some View {
        if let feedCount = loadingState?.refreshedFeedCount, let totalFeedCount = loadingState?.totalFeedCount {

            if showLoading {

                let feedRefreshCounter = "\(feedCount)/\(totalFeedCount)"

                let loadingFeedString = LocalizationUtils.shared.formatString(
                    resource: MR.strings().loading_feed_message,
                    args: [feedRefreshCounter]
                )
                Text(loadingFeedString)
                    .font(.body)
                    .accessibilityIdentifier(TestingTag.shared.LOADING_BAR)
            }
        }
    }

}

#Preview {
    FeedListView(
        loadingState: FinishedFeedUpdateStatus(),
        feedState: PreviewItemsKt.feedItemsForPreview,
        showLoading: false,
        currentFeedFilter: FeedFilter.Timeline(),
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in }
    ).environmentObject(HomeListIndexHolder())
}

#Preview {
    FeedListView(
        loadingState: NoFeedSourcesStatus(),
        feedState: [],
        showLoading: false,
        currentFeedFilter: FeedFilter.Timeline(),
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in }
    )
}

#Preview {
    FeedListView(
        loadingState: FinishedFeedUpdateStatus(),
        feedState: [],
        showLoading: false,
        currentFeedFilter: FeedFilter.Timeline(),
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in }
    )
}
