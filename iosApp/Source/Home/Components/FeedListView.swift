//
//  FeedListView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import Reeeed
import SwiftUI

struct FeedListView: View {
    @Environment(\.openURL) private var openURL
    @Environment(HomeListIndexHolder.self) private var indexHolder
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(AppState.self) private var appState

    @State private var browserToOpen: BrowserToPresent?

    var loadingState: FeedUpdateStatus?
    var feedState: [FeedItem]
    var showLoading: Bool
    let currentFeedFilter: FeedFilter
    let columnVisibility: NavigationSplitViewVisibility
    let feedFontSizes: FeedFontSizes

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
                        Button(
                            action: {
                                let urlInfo = FeedItemUrlInfo(
                                    id: feedItem.id,
                                    url: feedItem.url,
                                    title: feedItem.title,
                                    openOnlyOnBrowser: false,
                                    isBookmarked: feedItem.isBookmarked,
                                    linkOpeningPreference: feedItem.feedSource.linkOpeningPreference
                                )

                                switch urlInfo.linkOpeningPreference {
                                case .readerMode:
                                    self.appState.navigate(route: CommonViewRoute.readerMode(feedItem: feedItem))
                                case .internalBrowser:
                                    browserToOpen = .inAppBrowser(url: URL(string: feedItem.url)!)
                                case .preferredBrowser:
                                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                                case .default:
                                    if browserSelector.openReaderMode(link: feedItem.url) {
                                        self.appState.navigate(route: CommonViewRoute.readerMode(feedItem: feedItem))
                                    } else if browserSelector.openInAppBrowser() {
                                        browserToOpen = .inAppBrowser(url: URL(string: feedItem.url)!)
                                    } else {
                                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                                    }
                                }
                                onItemClick(urlInfo)
                            },
                            label: {
                                FeedItemView(feedItem: feedItem, index: index, feedFontSizes: feedFontSizes)
                                    .contentShape(Rectangle())
                            }
                        )
                        .buttonStyle(.plain)
                        .id(feedItem.id)
                        .listRowInsets(EdgeInsets())
                        .hoverEffect()
                        .contextMenu {
                            VStack {
                                makeReadUnreadButton(feedItem: feedItem)
                                makeBookmarkButton(feedItem: feedItem)
                                makeCommentsButton(feedItem: feedItem)
                                makeShareButton(feedItem: feedItem)
                                if let commentUrl = feedItem.commentsUrl {
                                    makeShareCommentsButton(commentsUrl: commentUrl)
                                }
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
                .fullScreenCover(item: $browserToOpen) { browserToOpen in
                    switch browserToOpen {
                    case let .inAppBrowser(url):
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
    private func makeShareButton(feedItem: FeedItem) -> some View {
        ShareLink(
            item: URL(string: feedItem.url)!,
            message: Text(feedItem.title ?? ""),
            label: {
                Label(feedFlowStrings.menuShare, systemImage: "square.and.arrow.up")
            }
        )
    }

    @ViewBuilder
    private func makeShareCommentsButton(commentsUrl: String) -> some View {
        ShareLink(
            item: URL(string: commentsUrl)!,
            label: {
                Label(feedFlowStrings.menuShareComments, systemImage: "square.and.arrow.up.on.square")
            }
        )
    }

    @ViewBuilder
    private var loadingHeader: some View {
        if let feedCount = loadingState?.refreshedFeedCount,
           let totalFeedCount = loadingState?.totalFeedCount {
            if showLoading {
                if feedCount > 0 && totalFeedCount > 0 {
                    let feedRefreshCounter = "\(feedCount)/\(totalFeedCount)"

                    Text(feedFlowStrings.loadingFeedMessage(feedRefreshCounter))
                        .font(.body)
                        .accessibilityIdentifier(TestingTag.shared.LOADING_BAR)
                } else {
                    Text(feedFlowStrings.loadingFeedMessage("..."))
                        .font(.body)
                        .accessibilityIdentifier(TestingTag.shared.LOADING_BAR)
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
