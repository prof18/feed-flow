//
//  HomeScreenContent+Toolbar.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/26.
//  Copyright © 2026. All rights reserved.
//

import FeedFlowKit
import SwiftUI

extension HomeContent {
    @ToolbarContentBuilder
    func makeAdaptiveCompactToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button(action: showDrawer) {
                Label(feedFlowStrings.drawerMenuButtonContentDescription, systemImage: "sidebar.leading")
            }
            .accessibilityIdentifier(DrawerAccessibilityIdentifiers.menuButton)
        }
        makeIOS26ToolbarContent(proxy: proxy)
    }

    @ToolbarContentBuilder
    func makeIOS26ToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        if #available(iOS 27.1, *) {
            HomeAdaptiveToolbar(
                search: makeSearchToolbarButton(),
                more: makeMoreMenu(proxy: proxy)
            )
        } else {
            makeHorizontalToolbarContent(proxy: proxy)
        }
    }

    @ToolbarContentBuilder
    private func makeHorizontalToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarSpacer(.fixed)
        }

        ToolbarItem {
            makeSearchToolbarButton()
        }

        if #available(iOS 26.0, *) {
            ToolbarSpacer(.fixed)
        }

        makeMenuToolbarView(proxy: proxy)
    }

    @ToolbarContentBuilder
    func makeCompactPhoneToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        makeCompactPhoneHeaderToolbarView()

        if isiOS26OrLater() {
            if #available(iOS 26.0, *) {
                ToolbarSpacer(.fixed)
            }

            ToolbarItem {
                Button {
                    self.appState.navigate(
                        route: searchRoute
                    )
                } label: {
                    Label(feedFlowStrings.searchButtonContentDescription, systemImage: "magnifyingglass")
                }
                .accessibilityIdentifier(HomeToolbarAccessibilityIdentifiers.searchButton)
            }

            if #available(iOS 26.0, *) {
                ToolbarSpacer(.fixed)
            }
        } else {
            makeSearchToolbarView()
        }

        makeMenuToolbarView(proxy: proxy)
    }

    @ToolbarContentBuilder
    func makeLegacyToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        makeToolbarHeaderView()

        makeSearchToolbarView()
        makeMenuToolbarView(proxy: proxy)
    }

    @ToolbarContentBuilder
    func makeCompactPhoneHeaderToolbarView() -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            Button(action: showDrawer) {
                HStack(spacing: Spacing.small) {
                    Image(systemName: "sidebar.leading")

                    Rectangle()
                        .fill(Color.secondary.opacity(0.35))
                        .frame(width: 1, height: 28)

                    makeCompactPhoneHeaderText()
                        .frame(maxWidth: compactPhoneHeaderMaxWidth, alignment: .leading)
                        .padding(.trailing, Spacing.small)
                        .layoutPriority(1)
                }
                .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
                .fixedSize(horizontal: true, vertical: true)
            }
            .accessibilityIdentifier(DrawerAccessibilityIdentifiers.menuButton)
            .accessibilityLabel(
                "\(feedFlowStrings.drawerMenuButtonContentDescription), \(compactPhoneHeaderAccessibilityLabel)"
            )
            .layoutPriority(1)
        }
    }

    var compactPhoneHeaderMaxWidth: CGFloat {
        let dynamicTypeWidth: CGFloat
        if dynamicTypeSize.isAccessibilitySize {
            dynamicTypeWidth = 56
        } else if dynamicTypeSize >= .xLarge {
            dynamicTypeWidth = 24
        } else {
            dynamicTypeWidth = 0
        }

        return compactToolbarHeaderWidth(verticalSizeClass: verticalSizeClass) + dynamicTypeWidth
    }

    private var searchRoute: CommonViewRoute {
        .search
    }

    private var shouldShowUnreadCount: Bool {
        !isUnreadCountHidden &&
            !(currentFeedFilter is FeedFilter.Read) &&
            !(currentFeedFilter is FeedFilter.Bookmarks)
    }

    func makeCompactPhoneHeaderText() -> some View {
        HStack(alignment: .center, spacing: Spacing.small) {
            compactPhoneHeaderTitle
                .layoutPriority(1)

            if shouldShowUnreadCount {
                UnreadCountBadge(count: unreadCount)
                    .layoutPriority(2)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(compactPhoneHeaderAccessibilityLabel)
    }

    var compactPhoneHeaderTitle: some View {
        Text(getCompactToolbarTitle(feedFilter: currentFeedFilter))
            .font(.headline)
            .foregroundStyle(.primary)
            .lineLimit(1)
            .truncationMode(.tail)
    }

    var compactPhoneHeaderAccessibilityLabel: String {
        let title = getCompactToolbarTitle(feedFilter: currentFeedFilter)
        if shouldShowUnreadCount {
            return "\(title), \(unreadCount.formatted())"
        } else {
            return title
        }
    }

    @ToolbarContentBuilder
    func makeToolbarHeaderView() -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            HStack {
                Text(getNavBarName(feedFilter: currentFeedFilter))
                    .font(.title2)
                    .lineLimit(1)
                    .truncationMode(.tail)

                if shouldShowUnreadCount {
                    Text("(\(unreadCount))")
                        .font(.title2)
                }
            }
            .padding(.vertical, Spacing.medium)
        }
    }

    @ToolbarContentBuilder
    func makeSearchToolbarView() -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            Button {
                self.appState.navigate(
                    route: searchRoute
                )
            } label: {
                Label(feedFlowStrings.searchButtonContentDescription, systemImage: "magnifyingglass")
            }
            .accessibilityIdentifier(HomeToolbarAccessibilityIdentifiers.searchButton)
        }
    }

    private func makeSearchToolbarButton() -> some View {
        Button {
            appState.navigate(route: searchRoute)
        } label: {
            Label(feedFlowStrings.searchButtonContentDescription, systemImage: "magnifyingglass")
        }
        .accessibilityIdentifier(HomeToolbarAccessibilityIdentifiers.searchButton)
    }

    @ToolbarContentBuilder
    func makeMenuToolbarView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(placement: .primaryAction) {
            makeMoreMenu(proxy: proxy)
        }
    }

    private func makeMoreMenu(proxy: ScrollViewProxy) -> some View {
        Menu {
            makeMenuActions(proxy: proxy)
        } label: {
            Label(
                feedFlowStrings.moreOptionsButtonContentDescription,
                systemImage: isiOS26OrLater() ? "ellipsis" : "ellipsis.circle"
            )
        }
        .accessibilityIdentifier(HomeToolbarAccessibilityIdentifiers.moreMenuButton)
    }

    @ViewBuilder
    func makeMenuActions(proxy: ScrollViewProxy) -> some View {
        if showFeedSyncButton {
            Button {
                self.onFeedSyncClick()
            } label: {
                Label(feedFlowStrings.triggerFeedSync, systemImage: "square.and.arrow.up")
            }
        }

        Button {
            requestMarkAllReadConfirmation()
        } label: {
            Label(feedFlowStrings.markAllReadButton, systemImage: "text.badge.checkmark")
        }
        .accessibilityIdentifier(HomeToolbarAccessibilityIdentifiers.markAllReadMenuItem)

        Button {
            requestViewOptions()
        } label: {
            Label(feedFlowStrings.sortAndFilterButton, systemImage: "line.3.horizontal.decrease")
        }

        if let source = (currentFeedFilter as? FeedFilter.Source)?.feedSource {
            Button {
                self.sheetToShow = .editFeed(source)
            } label: {
                Label(feedFlowStrings.openFeedSettings, systemImage: "gearshape")
            }
        }

        Divider()

        Button(role: .destructive) {
            requestClearOldArticlesConfirmation()
        } label: {
            Label(feedFlowStrings.clearOldArticlesButton, systemImage: "trash")
        }

        #if DEBUG
            Divider()

            Button {
                deleteAllFeeds()
            } label: {
                Label("Delete Database", systemImage: "trash")
            }
        #endif
    }

    func getNavBarTitleWithCount(feedFilter: FeedFilter, unreadCount: Int) -> String {
        let baseName = getNavBarName(feedFilter: feedFilter)

        if !isUnreadCountHidden &&
            !(feedFilter is FeedFilter.Read) &&
            !(feedFilter is FeedFilter.Bookmarks) {
            return "\(baseName) (\(unreadCount))"
        } else {
            return baseName
        }
    }

    func getCompactToolbarTitle(feedFilter: FeedFilter) -> String {
        switch feedFilter {
        case let category as FeedFilter.Category:
            return category.feedCategory.title

        case let source as FeedFilter.Source:
            return source.feedSource.title

        default:
            return getNavBarName(feedFilter: feedFilter)
        }
    }

    func getNavBarName(feedFilter: FeedFilter) -> String {
        switch feedFilter {
        case let category as FeedFilter.Category:
            return category.feedCategory.title

        case let source as FeedFilter.Source:
            return source.feedSource.title

        case is FeedFilter.Timeline:
            return feedFlowStrings.drawerTitleTimeline

        case is FeedFilter.Read:
            return feedFlowStrings.drawerTitleRead

        case is FeedFilter.Bookmarks:
            return feedFlowStrings.drawerTitleBookmarks

        default:
            return feedFlowStrings.appName
        }
    }
}
