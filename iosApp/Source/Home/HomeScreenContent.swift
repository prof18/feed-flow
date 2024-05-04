//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 15/01/24.
//  Copyright © 2024. All rights reserved.
//

import SwiftUI
import shared

struct HomeContent: View {

    @EnvironmentObject private var indexHolder: HomeListIndexHolder
    @EnvironmentObject private var appState: AppState

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    @Binding var loadingState: FeedUpdateStatus?
    @Binding var feedState: [FeedItem]
    @Binding var showLoading: Bool
    @Binding var unreadCount: Int
    @Binding var sheetToShow: HomeSheetToShow?
    @Binding var toggleListScroll: Bool
    @Binding var currentFeedFilter: FeedFilter
    @Binding var showSettings: Bool

    let onRefresh: () -> Void
    let updateReadStatus: (Int32) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let onForceRefreshClick: () -> Void
    let deleteAllFeeds: () -> Void
    let requestNewPage: () -> Void
    let onItemClick: (FeedItemUrlInfo) -> Void
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void
    let onBackToTimelineClick: () -> Void

    var body: some View {
        ScrollViewReader { proxy in
            FeedListView(
                loadingState: loadingState,
                feedState: feedState,
                showLoading: showLoading,
                currentFeedFilter: currentFeedFilter,
                onReloadClick: onRefresh,
                onAddFeedClick: {
                    // It's crashing with "Application tried to present modally
                    // a view controller that is already being presented" on iOS 16
                    if #unavailable(iOS 17.0) {
                        // only runs if <iOS 15
                        self.sheetToShow = .settings
                    } else {
                        self.sheetToShow = .noFeedSource
                    }
                },
                requestNewPage: requestNewPage,
                onItemClick: onItemClick,
                onBookmarkClick: onBookmarkClick,
                onReadStatusClick: onReadStatusClick,
                onBackToTimelineClick: onBackToTimelineClick
            )
            .onChange(of: toggleListScroll) { _ in
                proxy.scrollTo(feedState.first?.id)
            }
            .onChange(of: showSettings) { _ in
                sheetToShow = .settings
            }
            .if(appState.sizeClass == .compact) { view in
                view.navigationBarBackButtonHidden(true)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                makeToolbarHeaderView(proxy: proxy)
                if !isOnVisionOSDevice() {
                    makeSearchToolbarView()
                    makeMenuToolbarView(proxy: proxy)
                }
            }

        }
        .sheet(item: $sheetToShow) { item in
            switch item {
            case .settings:
                SettingsScreen()

            case .noFeedSource:
                NoFeedsBottomSheet(
                    onAddFeedClick: {
                        self.sheetToShow = .addFeed
                    },
                    onImportExportClick: {
                        self.sheetToShow = .importExport
                    }
                )
            case .addFeed:
                AddFeedScreen(showCloseButton: true)

            case .importExport:
                ImportExportScreen(showCloseButton: true)
            }
        }
    }

    @ToolbarContentBuilder
    private func makeToolbarHeaderView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(id: UUID().uuidString, placement: .navigationBarLeading, showsByDefault: true) {
            HStack {
                if appState.sizeClass == .compact {
                    Button {
                        self.dismiss()
                    } label: {
                        Image(systemName: "sidebar.left")
                    }
                    .accessibilityIdentifier(TestingTag.shared.DRAWER_MENU_BUTTON)
                }

                HStack {
                    Text(currentFeedFilter.getNavBarName())
                        .font(.title2)

                    if !(currentFeedFilter is FeedFilter.Read) &&
                        !(currentFeedFilter is FeedFilter.Bookmarks) {
                        Text("(\(unreadCount))")
                            .font(.title2)
                    }
                }
                .padding(.vertical, Spacing.medium)
                .onTapGesture(count: 2) {
                    onRefresh()
                    proxy.scrollTo(feedState.first?.id)
                    updateReadStatus(Int32(indexHolder.getLastReadIndex()))
                    self.indexHolder.refresh()
                }
                .accessibilityIdentifier(TestingTag.shared.HOME_TOOLBAR)
            }
        }
    }

    @ToolbarContentBuilder
    private func makeSearchToolbarView() -> some ToolbarContent {
        ToolbarItem(id: UUID().uuidString, placement: .navigationBarTrailing) {
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
    private func makeMenuToolbarView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(id: UUID().uuidString, placement: .primaryAction, showsByDefault: true) {
            Menu {
                Button {
                    onMarkAllReadClick()
                } label: {
                    Label(feedFlowStrings.markAllReadButton, systemImage: "checkmark")
                }

                Button {
                    onDeleteOldFeedClick()
                } label: {
                    Label(feedFlowStrings.clearOldArticlesButton, systemImage: "trash")
                }

                Button {
                    proxy.scrollTo(feedState.first?.id)
                    onForceRefreshClick()
                } label: {
                    Label(feedFlowStrings.forceFeedRefresh, systemImage: "arrow.clockwise")
                }

                Button {
                    self.sheetToShow = .settings
                } label: {
                    Label(feedFlowStrings.settingsButton, systemImage: "gear")
                }
                .accessibilityLabel(TestingTag.shared.SETTINGS_MENU)

                #if DEBUG
                Button {
                    deleteAllFeeds()
                } label: {
                    Label("Delete Database", systemImage: "trash")
                }
                #endif

            } label: {
                Image(systemName: "ellipsis.circle")
            }
            .accessibilityIdentifier(TestingTag.shared.SETTING_BUTTON)
        }
    }
}

fileprivate extension FeedFilter {
    func getNavBarName() -> String {
        switch self {
        case let category as FeedFilter.Category:
            return category.feedCategory.title

        case let source as FeedFilter.Source:
            return source.feedSource.title

        case is FeedFilter.Read:
            return feedFlowStrings.drawerTitleRead

        case is FeedFilter.Bookmarks:
            return feedFlowStrings.drawerTitleBookmarks

        default:
            return feedFlowStrings.appName
        }
    }
}

#Preview("HomeContentLoading") {
    HomeContent(
        loadingState: .constant(
            InProgressFeedUpdateStatus(
                refreshedFeedCount: Int32(10),
                totalFeedCount: Int32(42)
            )
        ),
        feedState: .constant(PreviewItemsKt.feedItemsForPreview),
        showLoading: .constant(true),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        onRefresh: {},
        updateReadStatus: { _ in },
        onMarkAllReadClick: {},
        onDeleteOldFeedClick: {},
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {}
    )
        .environmentObject(HomeListIndexHolder())
        .environmentObject(AppState())
        .environmentObject(BrowserSelector())
}

#Preview("HomeContentLoaded") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(PreviewItemsKt.feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        onRefresh: {},
        updateReadStatus: { _ in },
        onMarkAllReadClick: {},
        onDeleteOldFeedClick: {},
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {}
    )
        .environmentObject(HomeListIndexHolder())
        .environmentObject(AppState())
        .environmentObject(BrowserSelector())
}

#Preview("HomeContentSettings") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(PreviewItemsKt.feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(HomeSheetToShow.noFeedSource),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        onRefresh: {},
        updateReadStatus: { _ in },
        onMarkAllReadClick: {},
        onDeleteOldFeedClick: {},
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onBackToTimelineClick: {}
    )
        .environmentObject(HomeListIndexHolder())
        .environmentObject(AppState())
        .environmentObject(BrowserSelector())
}
