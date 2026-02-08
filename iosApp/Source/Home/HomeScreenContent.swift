//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 15/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct HomeContent: View {
    @Environment(HomeListIndexHolder.self)
    private var indexHolder
    @Environment(AppState.self)
    private var appState
    @Environment(\.scenePhase)
    private var scenePhase: ScenePhase

    @Environment(\.dismiss)
    private var dismiss
    @Environment(\.openURL)
    private var openURL

    @Binding var loadingState: FeedUpdateStatus?
    @Binding var feedState: [FeedItem]
    @Binding var showLoading: Bool
    @Binding var unreadCount: Int
    @Binding var sheetToShow: HomeSheetToShow?
    @Binding var toggleListScroll: Bool
    @Binding var currentFeedFilter: FeedFilter
    @Binding var showSettings: Bool
    @Binding var showFeedSyncButton: Bool
    @Binding var columnVisibility: NavigationSplitViewVisibility
    @Binding var feedFontSizes: FeedFontSizes
    @Binding var swipeActions: SwipeActions
    @Binding var feedLayout: FeedLayout

    @State var isToolbarVisible = true
    @State var showScrollToTop = false
    @State var showMarkAllReadDialog = false
    @State var showClearOldArticlesDialog = false

    let onRefresh: () -> Void
    let updateReadStatus: (Int32) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let onForceRefreshClick: () -> Void
    let deleteAllFeeds: () -> Void
    let requestNewPage: () -> Void
    let onItemClick: (FeedItemUrlInfo) -> Void
    let onReaderModeClick: (FeedItemUrlInfo) -> Void
    let onBookmarkClick: (FeedItemId, Bool) -> Void
    let onReadStatusClick: (FeedItemId, Bool) -> Void
    let onMarkAllAboveAsRead: (String) -> Void
    let onMarkAllBelowAsRead: (String) -> Void
    let onBackToTimelineClick: () -> Void
    let onFeedSyncClick: () -> Void
    let openDrawer: () -> Void

    var body: some View {
        ScrollViewReader { proxy in
            feedListView(proxy: proxy)
        }
        .alert(feedFlowStrings.markAllReadButton, isPresented: $showMarkAllReadDialog) {
            Button(feedFlowStrings.cancelButton, role: .cancel) {}
            Button(feedFlowStrings.confirmButton) {
                onMarkAllReadClick()
            }
        } message: {
            Text(feedFlowStrings.markAllReadDialogMessage)
        }
        .alert(feedFlowStrings.clearOldArticlesButton, isPresented: $showClearOldArticlesDialog) {
            Button(feedFlowStrings.cancelButton, role: .cancel) {}
            Button(feedFlowStrings.confirmButton) {
                onDeleteOldFeedClick()
            }
        } message: {
            Text(feedFlowStrings.clearOldArticlesDialogMessage)
        }
        .onChange(of: appState.redrawAfterFeedSourceEdit) {
            onRefresh()
        }
        .onChange(of: scenePhase) {
            if UIDevice.current.userInterfaceIdiom == .pad {
                switch scenePhase {
                case .active:
                    isToolbarVisible = true
                case .background:
                    isToolbarVisible = false
                default:
                    break
                }
            }
        }
        .sheet(item: $sheetToShow) { item in
            sheetContent(item)
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowRefreshFeeds)) { _ in
            onRefresh()
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowForceRefreshFeeds)) { _ in
            toggleListScroll.toggle()
            onForceRefreshClick()
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowMarkAllRead)) { _ in
            showMarkAllReadDialog = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowClearOldArticles)) { _ in
            showClearOldArticlesDialog = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowAddFeed)) { _ in
            sheetToShow = .addFeed
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowEditCurrentFeed)) { _ in
            if let source = (currentFeedFilter as? FeedFilter.Source)?.feedSource {
                sheetToShow = .editFeed(source)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowImportExport)) { _ in
            sheetToShow = .importExport
        }
        .onReceive(NotificationCenter.default.publisher(for: .feedFlowOpenSettings)) { _ in
            sheetToShow = .settings
        }
    }
}

// MARK: - HomeContent Toolbar Extension
private extension HomeContent {
    var feedListBaseView: some View {
        FeedListView(
            loadingState: loadingState,
            feedState: feedState,
            showLoading: showLoading,
            currentFeedFilter: currentFeedFilter,
            columnVisibility: columnVisibility,
            feedFontSizes: feedFontSizes,
            swipeActions: swipeActions,
            feedLayout: feedLayout,
            onReloadClick: onRefresh,
            onAddFeedClick: {
                self.sheetToShow = .noFeedSource
            },
            requestNewPage: requestNewPage,
            onItemClick: onItemClick,
            onReaderModeClick: onReaderModeClick,
            onBookmarkClick: onBookmarkClick,
            onReadStatusClick: onReadStatusClick,
            onMarkAllAboveAsRead: onMarkAllAboveAsRead,
            onMarkAllBelowAsRead: onMarkAllBelowAsRead,
            onBackToTimelineClick: onBackToTimelineClick,
            onMarkAllAsReadClick: onMarkAllReadClick,
            openDrawer: openDrawer,
            onScrollPositionChanged: { shouldShow in
                withAnimation(.easeInOut(duration: 0.3)) {
                    showScrollToTop = shouldShow
                }
            },
            onOpenFeedSettings: { feedSource in
                sheetToShow = .editFeed(feedSource)
            }
        )
    }

    func feedListView(proxy: ScrollViewProxy) -> some View {
        feedListBaseView
            .onChange(of: toggleListScroll) {
                proxy.scrollTo(feedState.first?.id)
                showScrollToTop = false
            }
            .onChange(of: showSettings) {
                sheetToShow = .settings
            }
            .if(appState.sizeClass == .compact) { view in
                view.navigationBarBackButtonHidden(true)
            }
            .navigationBarTitleDisplayMode(.inline)
            .if(isiOS26OrLater()) { view in
                view.navigationTitle(getNavBarTitleWithCount(feedFilter: currentFeedFilter, unreadCount: unreadCount))
            }
            .toolbar {
                if isToolbarVisible {
                    if isiOS26OrLater() {
                        makeIOS26ToolbarContent(proxy: proxy)
                    } else {
                        makeLegacyToolbarContent(proxy: proxy)
                    }
                }
            }
            .showsScrollToTop(isVisible: showScrollToTop, onScrollToTop: {
                withAnimation {
                    proxy.scrollTo(feedState.first?.id)
                    showScrollToTop = false
                }
            })
    }

    @ViewBuilder
    func sheetContent(_ item: HomeSheetToShow) -> some View {
        switch item {
        case .settings:
            SettingsScreen(fetchFeeds: onRefresh)
                .environment(appState)
                .preferredColorScheme(appState.colorScheme)

        case .noFeedSource:
            NoFeedsBottomSheet(
                onAddFeedClick: {
                    self.sheetToShow = .addFeed
                },
                onImportExportClick: {
                    self.sheetToShow = .importExport
                },
                onFeedSuggestionsClick: {
                    self.sheetToShow = nil
                    appState.navigate(route: CommonViewRoute.feedSuggestions)
                }
            )

        case .addFeed:
            AddFeedScreen(showCloseButton: true)

        case .importExport:
            ImportExportScreen(showCloseButton: true, fetchFeeds: onRefresh)

        case let .editFeed(source):
            EditFeedScreen(feedSource: source)
        }
    }

    @ToolbarContentBuilder
    func makeIOS26ToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        if appState.sizeClass == .compact {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    self.dismiss()
                } label: {
                    Image(systemName: "sidebar.left")
                }
            }
        }

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
    func makeLegacyToolbarContent(proxy: ScrollViewProxy) -> some ToolbarContent {
        makeToolbarHeaderView(proxy: proxy)

        makeSearchToolbarView()
        makeMenuToolbarView(proxy: proxy)
    }

    @ToolbarContentBuilder
    func makeToolbarHeaderView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            HStack {
                if appState.sizeClass == .compact {
                    Button {
                        self.dismiss()
                    } label: {
                        Image(systemName: "sidebar.left")
                    }
                }

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
                .onTapGesture(count: 2) {
                    updateReadStatus(Int32(indexHolder.getLastReadIndex()))
                    self.indexHolder.refresh()
                    proxy.scrollTo(feedState.first?.id)
                    onRefresh()
                }
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

        case is FeedFilter.Read:
            return feedFlowStrings.drawerTitleRead

        case is FeedFilter.Bookmarks:
            return feedFlowStrings.drawerTitleBookmarks

        default:
            return feedFlowStrings.appName
        }
    }
}
