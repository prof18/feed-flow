import FeedFlowKit
import Foundation
import SwiftUI

struct FeedItemContextMenu: View {
    @Environment(\.openURL)
    private var openURL
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(AppState.self)
    private var appState

    let feedItem: FeedItem
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void
    let onMarkAllAboveAsRead: (String) -> Void
    let onMarkAllBelowAsRead: (String) -> Void
    let onOpenFeedSettings: (FeedSource) -> Void

    var body: some View {
        // 1. Open feed settings
        makeFeedSettingsButton(feedItem: feedItem)

        // Separator
        Divider()

        // 2. Mark all above as read
        makeMarkAllAboveAsReadButton(feedItem: feedItem)

        // 3. Mark all below as read
        makeMarkAllBelowAsReadButton(feedItem: feedItem)

        // Separator
        Divider()

        // Comments section (only if comments are available)
        if let commentUrl = feedItem.commentsUrl {
            // 4. Open comments
            makeCommentsButton(feedItem: feedItem)

            // 5. Share comments
            makeShareCommentsButton(commentsUrl: commentUrl)

            // Separator after comments section
            Divider()
        }

        // 6. Share
        makeShareButton(feedItem: feedItem)

        // 7. Add to bookmarks
        makeBookmarkButton(feedItem: feedItem)

        // 8. Mark as read (most frequent - at bottom for thumb reach)
        makeReadUnreadButton(feedItem: feedItem)
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
    private func makeMarkAllAboveAsReadButton(feedItem: FeedItem) -> some View {
        Button {
            onMarkAllAboveAsRead(feedItem.id)
        } label: {
            Label(feedFlowStrings.menuMarkAllAboveAsRead, systemImage: "chevron.up.2")
        }
    }

    @ViewBuilder
    private func makeMarkAllBelowAsReadButton(feedItem: FeedItem) -> some View {
        Button {
            onMarkAllBelowAsRead(feedItem.id)
        } label: {
            Label(feedFlowStrings.menuMarkAllBelowAsRead, systemImage: "chevron.down.2")
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
                if let url = URL(string: commentsUrl), browserSelector.openInAppBrowser() {
                    if browserSelector.isValidForInAppBrowser(url) {
                        appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                    } else {
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: commentsUrl))
                    }
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
            item: URL(string: feedItem.url) ?? URL(fileURLWithPath: ""),
            message: Text(feedItem.title ?? "")
        ) {
            Label(feedFlowStrings.menuShare, systemImage: "square.and.arrow.up")
        }
    }

    @ViewBuilder
    private func makeShareCommentsButton(commentsUrl: String) -> some View {
        ShareLink(
            item: URL(string: commentsUrl) ?? URL(fileURLWithPath: "")
        ) {
            Label(feedFlowStrings.menuShareComments, systemImage: "square.and.arrow.up.on.square")
        }
    }

    @ViewBuilder
    private func makeFeedSettingsButton(feedItem: FeedItem) -> some View {
        Button {
            onOpenFeedSettings(feedItem.feedSource)
        } label: {
            Label(feedFlowStrings.openFeedSettings, systemImage: "gearshape")
        }
    }
}
