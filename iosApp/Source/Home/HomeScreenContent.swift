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
    var indexHolder
    @Environment(AppState.self)
    var appState
    @Environment(\.scenePhase)
    private var scenePhase: ScenePhase
    @Environment(\.horizontalSizeClass)
    private var horizontalSizeClass

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
    @Binding var nextFeedPreviewState: NextFeedPreviewState
    @Binding var feedItemDisplaySettings: FeedItemDisplaySettings

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
    let onNavigateToNextFeed: () -> Void
    let onFeedSyncClick: () -> Void
    let openDrawer: () -> Void

    var isCompactPhone: Bool {
        horizontalSizeClass == .compact && UIDevice.current.userInterfaceIdiom == .phone
    }

    var shouldUseSystemNavigationTitle: Bool {
        isiOS26OrLater() && !isCompactPhone
    }

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
        .onReceive(NotificationCenter.default.publisher(for: .didReceiveNotificationDeepLink)) { _ in
            sheetToShow = nil
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
            feedItemDisplaySettings: feedItemDisplaySettings,
            nextFeedPreviewState: nextFeedPreviewState,
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
            onNavigateToNextFeed: onNavigateToNextFeed,
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
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(isCompactPhone)
            .if(shouldUseSystemNavigationTitle) { view in
                view.navigationTitle(getNavBarTitleWithCount(feedFilter: currentFeedFilter, unreadCount: unreadCount))
            }
            .toolbar {
                if isToolbarVisible {
                    if isCompactPhone {
                        makeCompactPhoneToolbarContent(proxy: proxy)
                    } else if isiOS26OrLater() {
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
                    self.sheetToShow = .feedSuggestions
                }
            )
            .environment(appState)

        case .addFeed:
            AddFeedScreen(showCloseButton: true)
                .environment(appState)
                .toggleStyle(BlueToggleStyle())

        case .importExport:
            ImportExportScreen(showCloseButton: true, fetchFeeds: onRefresh)
                .environment(appState)

        case let .editFeed(source):
            EditFeedScreen(feedSource: source)
                .environment(appState)
                .toggleStyle(BlueToggleStyle())

        case .feedSuggestions:
            FeedSuggestionsScreen()
        }
    }
}
