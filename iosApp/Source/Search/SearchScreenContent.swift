import FeedFlowKit
import Foundation
import SwiftUI

struct SearchScreenContent: View {
    @Environment(\.openURL)
    private var openURL
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(AppState.self)
    private var appState

    @Binding var searchText: String
    @Binding var searchState: SearchState
    @Binding var feedFontSizes: FeedFontSizes

    @State private var isPresented = true

    let readerModeViewModel: ReaderModeViewModel
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void
    let onMarkAllAboveAsRead: (String) -> Void
    let onMarkAllBelowAsRead: (String) -> Void

    var body: some View {
        makeSearchContent()
            .searchable(text: $searchText, isPresented: $isPresented, placement: .toolbar)
    }

    @ViewBuilder
    func makeSearchContent() -> some View {
        switch onEnum(of: searchState) {
        case .emptyState:
            ContentUnavailableView(
                label: {
                    Label(feedFlowStrings.searchHintTitle, systemImage: "magnifyingglass")
                },
                description: {
                    Text(feedFlowStrings.searchHintSubtitle)
                }
            )

        case let .noDataFound(state):
            ContentUnavailableView.search(text: state.searchQuery)

        case let .dataFound(state):
            makeSearchFoundContent(state: state)
        }
    }

    @ViewBuilder
    private func makeSearchFoundContent(state: SearchState.DataFound) -> some View {
        List {
            ForEach(Array(state.items.enumerated()), id: \.element) { index, feedItem in
                Button(action: {
                           if browserSelector.openReaderMode(link: feedItem.url) {
                               let urlInfo = FeedItemUrlInfo(
                                   id: feedItem.id,
                                   url: feedItem.url,
                                   title: feedItem.title,
                                   openOnlyOnBrowser: false,
                                   isBookmarked: feedItem.isBookmarked,
                                   linkOpeningPreference: feedItem.feedSource.linkOpeningPreference,
                                   commentsUrl: feedItem.commentsUrl
                               )
                               readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                               self.appState.navigate(route: CommonViewRoute.readerMode)
                           } else if browserSelector.openInAppBrowser() {
                               if let url = URL(string: feedItem.url) {
                                   if browserSelector.isValidForInAppBrowser(url) {
                                       appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                                   } else {
                                       openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                                   }
                               }
                           } else {
                               openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                           }
                           onReadStatusClick(FeedItemId(id: feedItem.id), true)
                       },
                       label: {
                           FeedItemView(feedItem: feedItem, index: index, feedFontSizes: feedFontSizes)
                       })
                       .buttonStyle(.plain)
                       .id(feedItem.id)
                       .contentShape(Rectangle())
                       .listRowInsets(EdgeInsets())
                       .hoverEffect()
                       .contextMenu {
                           VStack {
                               makeReadUnreadButton(feedItem: feedItem)
                               makeMarkAllAboveAsReadButton(feedItem: feedItem)
                               makeMarkAllBelowAsReadButton(feedItem: feedItem)
                               makeBookmarkButton(feedItem: feedItem)
                               makeCommentsButton(feedItem: feedItem)
                           }
                           .environment(browserSelector)
                       }
            }
        }
        .listStyle(PlainListStyle())
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
                if browserSelector.openInAppBrowser() {
                    if let url = URL(string: commentsUrl) {
                        if browserSelector.isValidForInAppBrowser(url) {
                            appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                        } else {
                            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: commentsUrl))
                        }
                    }
                } else {
                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: commentsUrl))
                }
            } label: {
                Label(feedFlowStrings.menuOpenComments, systemImage: "bubble.left.and.bubble.right")
            }
        }
    }
}
