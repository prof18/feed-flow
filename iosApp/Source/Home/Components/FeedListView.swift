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

    let onReloadClick: () -> Void
    let onAddFeedClick: () -> Void
    let requestNewPage: () -> Void
    let onItemClick: (FeedItemClickedInfo) -> Void

    var body: some View {
        if loadingState is NoFeedSourcesStatus {
            NoFeedsSourceView(onAddFeedClick: onAddFeedClick)
        } else if loadingState?.isLoading() == false && feedState.isEmpty {
            EmptyFeedView(onReloadClick: onReloadClick)
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
                    ForEach(feedState, id: \.self.id) { feedItem in
                        FeedItemView(feedItem: feedItem)
                            .id(feedItem.id)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                onItemClick(FeedItemClickedInfo(id: feedItem.id, url: feedItem.url))
                            }
                            .onLongPressGesture {
                                if let commentsUrl = feedItem.commentsUrl {
                                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: commentsUrl))
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
            }
        }
    }

}

#Preview {
    FeedListView(
        loadingState: FinishedFeedUpdateStatus(),
        feedState: PreviewItemsKt.feedItemsForPreview,
        showLoading: false,
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in }
    ).environmentObject(HomeListIndexHolder())
}

#Preview {
    FeedListView(
        loadingState: NoFeedSourcesStatus(),
        feedState: [],
        showLoading: false,
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in }
    )
}

#Preview {
    FeedListView(
        loadingState: FinishedFeedUpdateStatus(),
        feedState: [],
        showLoading: false,
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in }
    )
}
