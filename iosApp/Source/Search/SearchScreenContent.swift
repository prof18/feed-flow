import Foundation
import SwiftUI
import shared

struct SearchScreenContent: View {
    @Environment(\.openURL) private var openURL

    @EnvironmentObject private var browserSelector: BrowserSelector
    @EnvironmentObject private var appState: AppState

    @Binding var searchText: String
    @Binding var searchState: SearchState

    @State private var isPresented = true
    @State private var browserToOpen: BrowserToPresent?

    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void

    var body: some View {
        // TODO: Move to iOS 17 only
        if #available(iOS 17, *) {
            makeSearchContent()
                .searchable(text: $searchText, isPresented: $isPresented, placement: .toolbar)
        } else {
            makeSearchContent()
                .searchable(text: $searchText)
        }
    }

    @ViewBuilder
    func makeSearchContent() -> some View {
        switch onEnum(of: searchState) {
        case .emptyState:
            if #available(iOS 17, *) {
                ContentUnavailableView(
                    label: {
                        Label(feedFlowStrings.searchHintTitle, systemImage: "magnifyingglass")
                    },
                    description: {
                        Text(feedFlowStrings.searchHintSubtitle)
                    }
                )
            } else {
                VStack {
                    Text(feedFlowStrings.searchHintTitle)
                        .font(.title)

                    Text(feedFlowStrings.searchHintSubtitle)
                        .font(.body)
                }
            }

        case .noDataFound(let state):
            if #available(iOS 17, *) {
                ContentUnavailableView.search(text: state.searchQuery)
            } else {
                Text(feedFlowStrings.searchNoData(state.searchQuery))
            }

        case .dataFound(let state):
            makeSearchFoundContent(state: state)
        }
    }

    @ViewBuilder
    private func makeSearchFoundContent(state: SearchState.DataFound) -> some View {
        List {
            ForEach(Array(state.items.enumerated()), id: \.element) { index, feedItem in
                Button(action: {
                    if browserSelector.openReaderMode() {
                        self.appState.navigate(
                            route: CommonViewRoute.readerMode(url: URL(string: feedItem.url)!)
                        )
                    } else if browserSelector.openInAppBrowser() {
                        browserToOpen = .inAppBrowser(url: URL(string: feedItem.url)!)
                    } else {
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                    }
                    onReadStatusClick(FeedItemId(id: feedItem.id), true)
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
            }
        }
        .listStyle(PlainListStyle())
        .fullScreenCover(item: $browserToOpen) { browserToOpen in
            switch browserToOpen {
            case .inAppBrowser(let url):
                SFSafariView(url: url)
                    .ignoresSafeArea()
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
}
