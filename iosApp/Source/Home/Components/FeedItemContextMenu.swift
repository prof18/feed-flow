import FeedFlowKit
import Foundation
import SwiftUI

struct FeedItemContextMenu: View {
    @Environment(\.openURL) private var openURL
    @Environment(BrowserSelector.self) private var browserSelector

    let feedItem: FeedItem
    @Binding var browserToOpen: BrowserToPresent?
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void

    var body: some View {
        VStack {
            makeReadUnreadButton(feedItem: feedItem)
            makeBookmarkButton(feedItem: feedItem)
            makeCommentsButton(feedItem: feedItem)
            makeShareButton(feedItem: feedItem)
            if let commentUrl = feedItem.commentsUrl {
                makeShareCommentsButton(commentsUrl: commentUrl)
            }
            if isOnVisionOSDevice() {
                Button {
                    // No-op so it will close itself
                } label: {
                    Label(feedFlowStrings.closeMenuButton, systemImage: "xmark")
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
} 