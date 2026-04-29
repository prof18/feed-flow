//
//  CompactView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import Reader
import SwiftUI

struct CompactView: View {
    @Environment(AppState.self)
    private var appState
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(\.openURL)
    private var openURL

    @Binding var selectedSidebarItem: SidebarSelection?

    @State var navDrawerState: NavDrawerState = .init(
        timeline: [],
        read: [],
        bookmarks: [],
        categories: [],
        pinnedFeedSources: [],
        feedSourcesWithoutCategory: [],
        feedSourcesByCategory: [:]
    )
    @State var scrollUpTrigger = false
    @State var showSettings = false
    @State var showAddFeedSheet = false
    @State var showEditFeedSheet = false

    @State var indexHolder: HomeListIndexHolder
    let homeViewModel: HomeViewModel
    let readerModeViewModel: ReaderModeViewModel

    @State private var feedSourceToEdit: FeedSource?

    var body: some View {
        @Bindable var appState = appState

        NavigationStack(path: $appState.compactNavigationPath) {
            sidebar
                .navigationDestination(for: CompactViewRoute.self) { route in
                    switch route {
                    case .feed:
                        feedScreen
                    }
                }
                .navigationDestination(for: CommonViewRoute.self) { route in
                    commonDestination(route)
                }
        }
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showAddFeedSheet) {
            AddFeedScreen(showCloseButton: true)
                .environment(appState)
                .environment(browserSelector)
                .toggleStyle(BlueToggleStyle())
        }
        .sheet(isPresented: $showEditFeedSheet) {
            if let feedSource = feedSourceToEdit {
                EditFeedScreen(feedSource: feedSource)
                    .environment(appState)
                    .environment(browserSelector)
                    .toggleStyle(BlueToggleStyle())
            }
        }
        .onChange(of: appState.pendingBrowserURL) { _, newURL in
            if let url = newURL {
                appState.pendingBrowserURL = nil
                appState.currentCommonRoute = CommonViewRoute.inAppBrowser(url: url)
                appState.compactNavigationPath.append(CommonViewRoute.inAppBrowser(url: url))
            }
        }
        .onChange(of: appState.pendingExternalURL) { _, newURL in
            if let url = newURL {
                appState.pendingExternalURL = nil
                openURL(url)
            }
        }
        .onChange(of: appState.compactNavigationPath.count) { _, newCount in
            if newCount <= 1 {
                appState.currentCommonRoute = nil
            }
        }
        .task {
            for await state in homeViewModel.navDrawerState {
                self.navDrawerState = state
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .didReceiveNotificationDeepLink)) { _ in
            showAddFeedSheet = false
            showEditFeedSheet = false
            showSettings = false
            resetCompactNavigationToFeed()
        }
    }

    private var feedScreen: some View {
        HomeScreen(
            toggleListScroll: $scrollUpTrigger,
            showSettings: $showSettings,
            columnVisibility: .constant(.automatic),
            homeViewModel: homeViewModel,
            readerModeViewModel: readerModeViewModel,
            onReaderModeNavigate: nil,
            openDrawer: {
                if !appState.compactNavigationPath.isEmpty {
                    withAnimation {
                        appState.compactNavigationPath.removeLast()
                    }
                }
            },
            onSidebarSelectionChanged: { feedFilter in
                selectedSidebarItem = sidebarSelection(from: feedFilter)
            }
        )
        .environment(indexHolder)
        .environment(appState)
        .environment(browserSelector)
        .onAppear {
            appState.currentCommonRoute = nil
        }
    }

    private var sidebar: some View {
        SidebarDrawer(
            selectedSidebarItem: $selectedSidebarItem,
            navDrawerState: navDrawerState,
            onFeedFilterSelected: { feedFilter in
                indexHolder.clear()
                resetCompactNavigationToFeed()
                scrollUpTrigger.toggle()
                homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
            },
            onMarkAllReadClick: {
                homeViewModel.markAllRead()
            },
            onDeleteOldFeedClick: {
                homeViewModel.deleteOldFeedItems()
            },
            onForceRefreshClick: {
                scrollUpTrigger.toggle()
                homeViewModel.forceFeedRefresh()
            },
            deleteAllFeeds: {
                homeViewModel.deleteAllFeeds()
            },
            onShowSettingsClick: {
                resetCompactNavigationToFeed()
                showSettings.toggle()
            },
            onAddFeedClick: {
                showAddFeedSheet.toggle()
            },
            onEditFeedClick: { feedSource in
                feedSourceToEdit = feedSource
                showEditFeedSheet.toggle()
            },
            onDeleteFeedClick: { feedSource in
                homeViewModel.deleteFeedSource(feedSource: feedSource)
            },
            onPinFeedClick: { feedSource in
                homeViewModel.toggleFeedPin(feedSource: feedSource)
            },
            onDeleteCategory: { categoryId in
                homeViewModel.deleteCategory(categoryId: CategoryId(value: categoryId))
            },
            onUpdateCategoryName: { categoryId, categoryName in
                homeViewModel.updateCategoryName(
                    categoryId: CategoryId(value: categoryId),
                    newName: CategoryName(name: categoryName)
                )
            }
        )
        .environment(appState)
        .environment(browserSelector)
    }

    @ViewBuilder
    private func commonDestination(_ route: CommonViewRoute) -> some View {
        Group {
            switch route {
            case .readerMode:
                ReaderModeScreen(
                    viewModel: readerModeViewModel,
                    onInAppBrowserClick: nil
                )

            case .search:
                SearchScreen(
                    readerModeViewModel: readerModeViewModel,
                    onReaderModeNavigate: nil
                )

            case .accounts:
                AccountsScreen()

            case let .deepLinkFeed(feedId):
                DeepLinkFeedScreen(feedId: feedId, readerModeViewModel: readerModeViewModel)

            case let .inAppBrowser(url):
                SFSafariView(url: url)
                    .ignoresSafeArea()
                    .navigationBarBackButtonHidden(true)
            }
        }
        .onAppear {
            appState.currentCommonRoute = route
        }
    }

    private func resetCompactNavigationToFeed() {
        appState.currentCommonRoute = nil
        if appState.compactNavigationPath.isEmpty {
            appState.compactNavigationPath.append(CompactViewRoute.feed)
        } else {
            appState.compactNavigationPath = NavigationPath()
            appState.compactNavigationPath.append(CompactViewRoute.feed)
        }
    }

    private func sidebarSelection(from filter: FeedFilter) -> SidebarSelection? {
        if filter is FeedFilter.Timeline {
            return .timeline
        } else if filter is FeedFilter.Read {
            return .read
        } else if filter is FeedFilter.Bookmarks {
            return .bookmarks
        } else if let category = filter as? FeedFilter.Category {
            return .category(id: category.feedCategory.id)
        } else if let source = filter as? FeedFilter.Source {
            return .feedSource(id: source.feedSource.id)
        }
        return nil
    }
}
