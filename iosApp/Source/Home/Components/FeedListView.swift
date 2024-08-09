//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import FeedFlowKit
import Reeeed

struct FeedListView: View {
    @Environment(\.openURL) private var openURL

    @EnvironmentObject private var indexHolder: HomeListIndexHolder
    @EnvironmentObject private var browserSelector: BrowserSelector

    @EnvironmentObject private var appState: AppState

    @State private var browserToOpen: BrowserToPresent?

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
    let onBackToTimelineClick: () -> Void

    var body: some View {
        if loadingState is NoFeedSourcesStatus {
            NoFeedsSourceView(onAddFeedClick: onAddFeedClick)
        } else if loadingState?.isLoading() == false && feedState.isEmpty {
            EmptyFeedView(
                currentFeedFilter: currentFeedFilter,
                onReloadClick: onReloadClick,
                onBackToTimelineClick: onBackToTimelineClick
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
                        Button(action: {
                            if browserSelector.openReaderMode() {
                                self.appState.navigate(
                                    route: CommonViewRoute.readerMode(url: URL(string: feedItem.url)!)
                                )
                            } else if  browserSelector.openInAppBrowser() {
                                browserToOpen = .inAppBrowser(url: URL(string: feedItem.url)!)
                            } else {
                                openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                            }
                            onItemClick(
                                FeedItemUrlInfo(
                                    id: feedItem.id,
                                    url: feedItem.url,
                                    title: feedItem.title,
                                    openOnlyOnBrowser: false
                                )
                            )
                        },
                               label: {
                            FeedItemView(feedItem: feedItem, index: index)
                        })
                        .buttonStyle(.plain)
                        .id(feedItem.id)
                        .contentShape(Rectangle())
                        .listRowInsets(EdgeInsets())
                        .hoverEffect()
                        .contextMenu {
                            VStack {
                                makeReadUnreadButton(feedItem: feedItem)
                                makeBookmarkButton(feedItem: feedItem)
                                makeCommentsButton(feedItem: feedItem)
                                if isOnVisionOSDevice() {
                                    if isOnVisionOSDevice() {
                                        Button {
                                            // No-op so it will close itslef
                                        } label: {
                                            Label(feedFlowStrings.closeMenuButton, systemImage: "xmark")
                                        }
                                    }
                                }
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
                .fullScreenCover(item: $browserToOpen) { browserToOpen in
                    switch browserToOpen {
                    case .inAppBrowser(let url):
                        SFSafariView(url: url)
                            .ignoresSafeArea()
                    }
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
                Label(feedFlowStrings.menuMarkAsUnread, systemImage: "envelope.badge")
            } else {
                Label(feedFlowStrings.menuMarkAsRead, systemImage: "envelope.open")
            }
        }
    }

    @ViewBuilder
    private func makeBookmarkButton(feedItem: FeedItem) -> some View {
        Button {
            onBookmarkClick(FeedItemId(id: feedItem.id), !feedItem.isBookmarked)
        } label: {
            if feedItem.isBookmarked {
                Label(feedFlowStrings.menuRemoveFromBookmark, systemImage: "bookmark.slash")
            } else {
                Label(feedFlowStrings.menuAddToBookmark, systemImage: "bookmark")
            }
        }
    }

    @ViewBuilder
    private func makeCommentsButton(feedItem: FeedItem) -> some View {
        if let commentsUrl = feedItem.commentsUrl {
            Button {
                if browserSelector.openInAppBrowser() {
                    browserToOpen = .inAppBrowser(url: URL(string: commentsUrl)!)
                } else {
                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: commentsUrl))
                }
            } label: {
                Label(feedFlowStrings.menuOpenComments, systemImage: "bubble.left.and.bubble.right")
            }
        }
    }

    @ViewBuilder
    private var loadingHeader: some View {
        if let feedCount = loadingState?.refreshedFeedCount, let totalFeedCount = loadingState?.totalFeedCount {

            if showLoading {

                let feedRefreshCounter = "\(feedCount)/\(totalFeedCount)"

                Text(feedFlowStrings.loadingFeedMessage(feedRefreshCounter))
                    .font(.body)
                    .accessibilityIdentifier(TestingTag.shared.LOADING_BAR)
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
        onReloadClick: {},
        onAddFeedClick: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {}
    ).environmentObject(HomeListIndexHolder(fakeHomeViewModel: true))
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
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {}
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
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {}
    )
}
