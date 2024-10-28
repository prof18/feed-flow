import SwiftUI
import FeedFlowKit

struct SearchScreen: View {
    @Environment(\.openURL) private var openURL
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(AppState.self) private var appState
    
    @StateObject
    private var vmStoreOwner = VMStoreOwner<SearchViewModel>(Deps.shared.getSearchViewModel())

    @State var searchText = ""
    @State var searchState: SearchState = SearchState.EmptyState()
    @State private var isPresented = true
    @State private var browserToOpen: BrowserToPresent?

    var body: some View {
        makeSearchContent()
            .searchable(text: $searchText, isPresented: $isPresented, placement: .automatic)
            .onChange(of: searchText) {
                vmStoreOwner.instance.updateSearchQuery(query: searchText)
            }
            .task {
                for await state in vmStoreOwner.instance.searchQueryState {
                    self.searchText = state
                }
            }
            .task {
                for await state in vmStoreOwner.instance.searchState {
                    self.searchState = state
                }
            }
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

        case .noDataFound(let state):
            ContentUnavailableView.search(text: state.searchQuery)

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
                    vmStoreOwner.instance.onReadStatusClick(feedItemId: FeedItemId(id: feedItem.id), read: true)
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
            vmStoreOwner.instance.onReadStatusClick(feedItemId: FeedItemId(id: feedItem.id), read: !feedItem.isRead)
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
            vmStoreOwner.instance.onBookmarkClick(feedItemId: FeedItemId(id: feedItem.id), bookmarked: !feedItem.isBookmarked)
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
