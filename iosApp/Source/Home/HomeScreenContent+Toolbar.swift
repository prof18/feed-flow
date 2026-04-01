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
    func makeIOS26ToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarSpacer(.fixed)
        }

        ToolbarItem {
            Button {
                self.appState.navigate(
                    route: CommonViewRoute.search
                )
            } label: {
                Image(systemName: "magnifyingglass")
            }
        }

        if #available(iOS 26.0, *) {
            ToolbarSpacer(.fixed)
        }

        makeMenuToolbarView(proxy: proxy)
    }

    @ToolbarContentBuilder
    func makeCompactPhoneToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        makeCompactPhoneHeaderToolbarView(proxy: proxy)

        if isiOS26OrLater() {
            if #available(iOS 26.0, *) {
                ToolbarSpacer(.fixed)
            }

            ToolbarItem {
                Button {
                    self.appState.navigate(
                        route: CommonViewRoute.search
                    )
                } label: {
                    Image(systemName: "magnifyingglass")
                }
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
        makeToolbarHeaderView(proxy: proxy)

        makeSearchToolbarView()
        makeMenuToolbarView(proxy: proxy)
    }

    @ToolbarContentBuilder
    func makeCompactPhoneHeaderToolbarView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            HStack(spacing: Spacing.small) {
                Button {
                    openDrawer()
                } label: {
                    Image(systemName: "sidebar.leading")
                }

                Rectangle()
                    .fill(Color.secondary.opacity(0.35))
                    .frame(width: 1, height: 28)

                makeCompactPhoneHeaderText()
                    .frame(maxWidth: compactPhoneHeaderMaxWidth, alignment: .leading)
                    .padding(.trailing, Spacing.small)
                    .layoutPriority(1)
                    .contentShape(Rectangle())
                    .onTapGesture(count: 1) {
                        proxy.scrollTo(feedState.first?.id)
                    }
            }
            .fixedSize(horizontal: true, vertical: false)
            .layoutPriority(1)
        }
    }

    var compactPhoneHeaderMaxWidth: CGFloat {
        switch getDeviceType() {
        case .iphonePortrait:
            return 148
        case .iphoneLandscape:
            return 320
        case .ipad:
            return 220
        }
    }

    func makeCompactPhoneHeaderText() -> some View {
        VStack(alignment: .leading, spacing: Spacing.xxsmall) {
            Text(getCompactToolbarTitle(feedFilter: currentFeedFilter))
                .font(.headline)
                .foregroundStyle(.primary)
                .lineLimit(1)
                .truncationMode(.tail)

            Text(unreadCount.formatted())
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .monospacedDigit()
        }
    }

    @ToolbarContentBuilder
    func makeToolbarHeaderView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            HStack {
                Text(getNavBarName(feedFilter: currentFeedFilter))
                    .font(.title2)

                if !(currentFeedFilter is FeedFilter.Read) && !(currentFeedFilter is FeedFilter.Bookmarks) {
                    Text("(\(unreadCount))")
                        .font(.title2)
                }
            }
            .padding(.vertical, Spacing.medium)
            .onTapGesture(count: 1) {
                proxy.scrollTo(feedState.first?.id)
            }
        }
    }

    @ToolbarContentBuilder
    func makeSearchToolbarView() -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            Button {
                self.appState.navigate(
                    route: CommonViewRoute.search
                )
            } label: {
                Image(systemName: "magnifyingglass")
            }
        }
    }

    @ToolbarContentBuilder
    func makeMenuToolbarView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(placement: .primaryAction) {
            Menu {
                makeMenuActions(proxy: proxy)
            } label: {
                if isiOS26OrLater() {
                    Image(systemName: "ellipsis")
                } else {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
    }

    @ViewBuilder
    func makeMenuActions(proxy: ScrollViewProxy) -> some View {
        if showFeedSyncButton {
            Button {
                self.onFeedSyncClick()
            } label: {
                Label(feedFlowStrings.triggerFeedSync, systemImage: "arrow.uturn.up")
            }
        }

        Button {
            showMarkAllReadDialog = true
        } label: {
            Label(feedFlowStrings.markAllReadButton, systemImage: "checkmark")
        }

        Button {
            showClearOldArticlesDialog = true
        } label: {
            Label(feedFlowStrings.clearOldArticlesButton, systemImage: "trash")
        }

        Button {
            proxy.scrollTo(feedState.first?.id)
            onForceRefreshClick()
        } label: {
            Label(feedFlowStrings.forceFeedRefresh, systemImage: "arrow.clockwise")
        }

        if let source = (currentFeedFilter as? FeedFilter.Source)?.feedSource {
            Button {
                self.sheetToShow = .editFeed(source)
            } label: {
                Label(feedFlowStrings.editFeed, systemImage: "pencil")
            }
        }

        Button {
            self.sheetToShow = .settings
        } label: {
            Label(feedFlowStrings.settingsButton, systemImage: "gear")
        }

        #if DEBUG
            Button {
                deleteAllFeeds()
            } label: {
                Label("Delete Database", systemImage: "trash")
            }
        #endif
    }

    func getNavBarTitleWithCount(feedFilter: FeedFilter, unreadCount: Int) -> String {
        let baseName = getNavBarName(feedFilter: feedFilter)

        if !(feedFilter is FeedFilter.Read) && !(feedFilter is FeedFilter.Bookmarks) {
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
        let deviceType = getDeviceType()

        func getTruncatedTitle(_ title: String) -> String {
            switch deviceType {
            case .iphonePortrait:
                return title.truncate(maxChar: 12)
            case .ipad, .iphoneLandscape:
                return title.truncate(maxChar: 40)
            }
        }

        switch feedFilter {
        case let category as FeedFilter.Category:
            return getTruncatedTitle(category.feedCategory.title)

        case let source as FeedFilter.Source:
            return getTruncatedTitle(source.feedSource.title)

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
