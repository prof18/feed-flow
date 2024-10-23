//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 15/01/24.
//  Copyright © 2024. All rights reserved.
//

import SwiftUI
import FeedFlowKit

struct HomeContent: View {

    @Environment(HomeListIndexHolder.self) private var indexHolder
    @Environment(AppState.self) private var appState

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
    @Binding var showFeedSyncButton: Bool

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
    let onFeedSyncClick: () -> Void

    var body: some View {
        ScrollViewReader { proxy in
            FeedListView(
                loadingState: loadingState,
                feedState: feedState,
                showLoading: showLoading,
                currentFeedFilter: currentFeedFilter,
                onReloadClick: onRefresh,
                onAddFeedClick: {
                    self.sheetToShow = .noFeedSource
                },
                requestNewPage: requestNewPage,
                onItemClick: onItemClick,
                onBookmarkClick: onBookmarkClick,
                onReadStatusClick: onReadStatusClick,
                onBackToTimelineClick: onBackToTimelineClick
            )
            .onChange(of: toggleListScroll) {
                proxy.scrollTo(feedState.first?.id)
            }
            .onChange(of: showSettings) {
                sheetToShow = .settings
            }
            .if(appState.sizeClass == .compact) { view in
                view.navigationBarBackButtonHidden(true)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                makeToolbarHeaderView(proxy: proxy)
                if !isOnVisionOSDevice() {
                    if showFeedSyncButton {
                        makeFeedSynToolbarView()
                    }
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
        ToolbarItem(placement: .navigationBarLeading) {
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
                    Text(getNavBarName(feedFilter: currentFeedFilter))
                        .font(.title2)

                    if !(currentFeedFilter is FeedFilter.Read) &&
                        !(currentFeedFilter is FeedFilter.Bookmarks) {
                        Text("(\(unreadCount))")
                            .font(.title2)
                    }
                }
                .padding(.vertical, Spacing.medium)
                .onTapGesture(count: 1) {
                    proxy.scrollTo(feedState.first?.id)
                }
                .onTapGesture(count: 2) {
                    updateReadStatus(Int32(indexHolder.getLastReadIndex()))
                    self.indexHolder.refresh()
                    proxy.scrollTo(feedState.first?.id)
                    onRefresh()
                }
                .accessibilityIdentifier(TestingTag.shared.HOME_TOOLBAR)
            }
        }
    }

    @ToolbarContentBuilder
    private func makeFeedSynToolbarView() -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            Button {
                self.onFeedSyncClick()
            } label: {
                Image(systemName: "arrow.uturn.up")
            }
        }
    }

    @ToolbarContentBuilder
    private func makeSearchToolbarView() -> some ToolbarContent {
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
    private func makeMenuToolbarView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(placement: .primaryAction) {
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

        func getNavBarName(feedFilter: FeedFilter) -> String {
            let deviceType = getDeviceType()

            switch feedFilter {
            case let category as FeedFilter.Category:
                switch deviceType {
                case .ipad:
                    return category.feedCategory.title.truncate(maxChar: 40)

                case .iphonePortrait:
                    return category.feedCategory.title.truncate(maxChar: 12)

                case .iphoneLandscape:
                    return category.feedCategory.title.truncate(maxChar: 40)
                }

            case let source as FeedFilter.Source:
                switch deviceType {
                case .ipad:
                    return source.feedSource.title.truncate(maxChar: 40)

                case .iphonePortrait:
                    return source.feedSource.title.truncate(maxChar: 12)

                case .iphoneLandscape:
                    return source.feedSource.title.truncate(maxChar: 40)
                }

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
        feedState: .constant(feedItemsForPreview),
        showLoading: .constant(true),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        showFeedSyncButton: .constant(false),
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
        onBackToTimelineClick: {},
        onFeedSyncClick: {}
    )
        .environment(HomeListIndexHolder(fakeHomeViewModel: true))
        .environment(AppState())
        .environment(BrowserSelector())
}

#Preview("HomeContentLoaded") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        showFeedSyncButton: .constant(false),
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
        onBackToTimelineClick: {},
        onFeedSyncClick: {}
    )
        .environment(HomeListIndexHolder(fakeHomeViewModel: true))
        .environment(AppState())
        .environment(BrowserSelector())
}

#Preview("HomeContentSettings") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(HomeSheetToShow.noFeedSource),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        showFeedSyncButton: .constant(false),
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
        onBackToTimelineClick: {},
        onFeedSyncClick: {}
    )
        .environment(HomeListIndexHolder(fakeHomeViewModel: true))
        .environment(AppState())
        .environment(BrowserSelector())
}
