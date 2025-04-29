import FeedFlowKit
import Foundation
import SwiftUI

struct FeedItemRowView: View {
    @Environment(\.openURL) private var openURL
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(AppState.self) private var appState

    let feedItem: FeedItem
    let index: Int
    let feedFontSizes: FeedFontSizes
    let swipeActions: SwipeActions
    let onItemClick: (FeedItemUrlInfo) -> Void
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void

    var body: some View {
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

                guard let url = URL(string: feedItem.url) else {
                    // Handle invalid URL error appropriately
                    print("Invalid URL for feed item: \(feedItem.id)")
                    onItemClick(urlInfo) // Still call onItemClick if needed
                    return
                }

                switch urlInfo.linkOpeningPreference {
                case .readerMode:
                    self.appState.navigate(route: CommonViewRoute.readerMode(feedItem: feedItem))
                case .internalBrowser:
                    // Navigate instead of presenting modally
                    self.appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                case .preferredBrowser:
                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                case .default:
                    if browserSelector.openReaderMode(link: feedItem.url) {
                        self.appState.navigate(route: CommonViewRoute.readerMode(feedItem: feedItem))
                    } else if browserSelector.openInAppBrowser() {
                        // Navigate instead of presenting modally
                        self.appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
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
        .if(swipeActions.leftSwipeAction != .none) { view in
            view.swipeActions(edge: .trailing) {
                switch swipeActions.leftSwipeAction {
                case .toggleReadStatus:
                    Button {
                        onReadStatusClick(FeedItemId(id: feedItem.id), !feedItem.isRead)
                    } label: {
                        if feedItem.isRead {
                            Image(systemName: "envelope.badge")
                        } else {
                            Image(systemName: "envelope.open")
                        }
                    }
                case .toggleBookmarkStatus:
                    Button {
                        onBookmarkClick(FeedItemId(id: feedItem.id), !feedItem.isBookmarked)
                    } label: {
                        if feedItem.isBookmarked {
                            Image(systemName: "bookmark.slash")
                        } else {
                            Image(systemName: "bookmark")
                        }
                    }
                case .none:
                    EmptyView()
                @unknown default:
                    EmptyView()
                }
            }
        }
        .if(swipeActions.rightSwipeAction != .none) { view in
            view.swipeActions(edge: .leading) {
                switch swipeActions.rightSwipeAction {
                case .toggleReadStatus:
                    Button {
                        onReadStatusClick(FeedItemId(id: feedItem.id), !feedItem.isRead)
                    } label: {
                        if feedItem.isRead {
                            Image(systemName: "envelope.badge")
                        } else {
                            Image(systemName: "envelope.open")
                        }
                    }
                case .toggleBookmarkStatus:
                    Button {
                        onBookmarkClick(FeedItemId(id: feedItem.id), !feedItem.isBookmarked)
                    } label: {
                        if feedItem.isBookmarked {
                            Image(systemName: "bookmark.slash")
                        } else {
                            Image(systemName: "bookmark")
                        }
                    }
                case .none:
                    EmptyView()
                @unknown default:
                    EmptyView()
                }
            }
        }
        .contextMenu {
            FeedItemContextMenu(
                feedItem: feedItem,
                onBookmarkClick: onBookmarkClick,
                onReadStatusClick: onReadStatusClick
            )
        }
    }
}
