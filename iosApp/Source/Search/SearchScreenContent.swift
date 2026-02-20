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
    @Binding var searchFilter: SearchFilter
    let currentFeedFilter: FeedFilter?
    @Binding var feedFontSizes: FeedFontSizes

    @State private var isPresented = true

    let readerModeViewModel: ReaderModeViewModel
    let onSearchFilterSelected: (SearchFilter) -> Void
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void
    let onMarkAllAboveAsRead: (String) -> Void
    let onMarkAllBelowAsRead: (String) -> Void

    var body: some View {
        VStack(spacing: 0) {
            SearchFilterChipsRow(
                selectedFilter: searchFilter,
                currentFeedFilter: currentFeedFilter,
                onFilterSelected: { filter in
                    onSearchFilterSelected(filter)
                }
            )

            makeSearchContent()
        }
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
        let items = Array(state.items.enumerated())
        List {
            ForEach(items, id: \.element.id) { index, feedItem in
                makeSearchResultRow(feedItem: feedItem, index: index)
            }
        }
        .listStyle(PlainListStyle())
    }

    @ViewBuilder
    private func makeSearchResultRow(feedItem: FeedItem, index: Int) -> some View {
        Button {
            handleSearchItemTap(feedItem: feedItem)
        } label: {
            FeedItemView(feedItem: feedItem, index: index, feedFontSizes: feedFontSizes)
        }
        .buttonStyle(.plain)
        .id(feedItem.id)
        .contentShape(Rectangle())
        .listRowInsets(EdgeInsets())
        .hoverEffect()
        .contextMenu {
            makeSearchItemContextMenu(feedItem: feedItem)
                .environment(browserSelector)
        }
    }

    private func handleSearchItemTap(feedItem: FeedItem) {
        if browserSelector.openReaderMode(link: feedItem.url) {
            let urlInfo = FeedItemUrlInfo(
                id: feedItem.id,
                url: feedItem.url,
                title: feedItem.title,
                openOnlyOnBrowser: false,
                isBookmarked: feedItem.isBookmarked,
                linkOpeningPreference: feedItem.feedSource.linkOpeningPreference,
                commentsUrl: feedItem.commentsUrl,
                imageUrl: feedItem.imageUrl
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
    }

    @ViewBuilder
    private func makeSearchItemContextMenu(feedItem: FeedItem) -> some View {
        VStack {
            makeMarkAllAboveAsReadButton(feedItem: feedItem)
            makeMarkAllBelowAsReadButton(feedItem: feedItem)
            Divider()
            if feedItem.commentsUrl != nil {
                makeCommentsButton(feedItem: feedItem)
                Divider()
            }
            makeBookmarkButton(feedItem: feedItem)
            makeReadUnreadButton(feedItem: feedItem)
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

private struct SearchFilterChipsRow: View {
    let selectedFilter: SearchFilter
    let currentFeedFilter: FeedFilter?
    let onFilterSelected: (SearchFilter) -> Void

    private var currentFeedLabel: String? {
        currentFeedFilter?.label
    }

    private var filters: [SearchFilter] {
        if currentFeedLabel != nil {
            return [.currentFeed, .all, .read, .bookmarks]
        }
        return [.all, .read, .bookmarks]
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(filters, id: \.self) { filter in
                    SearchFilterChip(
                        label: filter.label(currentFeedLabel: currentFeedLabel),
                        isSelected: filter == selectedFilter,
                        onTap: {
                            onFilterSelected(filter)
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }
}

private struct SearchFilterChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(isSelected ? Color.accentColor.opacity(0.15) : Color.clear)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(
                            isSelected ? Color.accentColor : Color.secondary.opacity(0.3),
                            lineWidth: isSelected ? 1.5 : 1
                        )
                )
        }
        .buttonStyle(.plain)
    }
}

private extension SearchFilter {
    func label(currentFeedLabel: String?) -> String {
        switch self {
        case .currentFeed:
            return currentFeedLabel ?? feedFlowStrings.searchFilterAll
        case .all:
            return feedFlowStrings.searchFilterAll
        case .read:
            return feedFlowStrings.searchFilterRead
        case .bookmarks:
            return feedFlowStrings.searchFilterBookmarks
        }
    }
}

private extension FeedFilter {
    var label: String {
        switch self {
        case let category as FeedFilter.Category:
            return category.feedCategory.title
        case let source as FeedFilter.Source:
            return source.feedSource.title
        case is FeedFilter.Uncategorized:
            return feedFlowStrings.noCategory
        case is FeedFilter.Read:
            return feedFlowStrings.searchFilterRead
        case is FeedFilter.Bookmarks:
            return feedFlowStrings.searchFilterBookmarks
        default:
            return feedFlowStrings.searchFilterAll
        }
    }
}
