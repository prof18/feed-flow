//
//  RegularView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import Reader
import SwiftUI

struct ThreePaneView: View {
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

    @State var indexHolder: HomeListIndexHolder
    let homeViewModel: HomeViewModel
    let readerModeViewModel: ReaderModeViewModel

    @State private var showEditFeedSheet = false
    @State private var feedSourceToEdit: FeedSource?

    @State private var columnVisibility: NavigationSplitViewVisibility = .all
    @State private var preferredColumn: NavigationSplitViewColumn = .content
    @State private var detailContent: DetailPaneContent = .empty
    @State private var detailNavigationPath = NavigationPath()

    var body: some View {
        NavigationSplitView(
            columnVisibility: $columnVisibility,
            preferredCompactColumn: $preferredColumn
        ) {
            SidebarDrawer(
                selectedSidebarItem: $selectedSidebarItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    indexHolder.clear()
                    scrollUpTrigger.toggle()
                    detailContent = .empty
                    detailNavigationPath = NavigationPath()
                    preferredColumn = .content
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
            .navigationBarTitleDisplayMode(.inline)
        } content: {
            @Bindable var appState = appState
            NavigationStack(path: $appState.regularNavigationPath) {
                HomeScreen(
                    toggleListScroll: $scrollUpTrigger,
                    showSettings: $showSettings,
                    columnVisibility: $columnVisibility,
                    homeViewModel: homeViewModel,
                    readerModeViewModel: readerModeViewModel,
                    onReaderModeNavigate: {
                        detailContent = .readerMode
                        detailNavigationPath = NavigationPath()
                        preferredColumn = .detail
                    },
                    openDrawer: {
                        preferredColumn = .sidebar
                    }
                )
                .environment(indexHolder)
                .environment(appState)
                .environment(browserSelector)
                .navigationDestination(for: CommonViewRoute.self) { route in
                    switch route {
                    case .readerMode:
                        ReaderModeScreen(viewModel: readerModeViewModel, onInAppBrowserClick: nil)

                    case .search:
                        SearchScreen(
                            readerModeViewModel: readerModeViewModel,
                            onReaderModeNavigate: {
                                detailContent = .readerMode
                                detailNavigationPath = NavigationPath()
                                preferredColumn = .detail
                            }
                        )

                    case .accounts:
                        AccountsScreen()

                    case .dropboxSync:
                        DropboxSyncScreen()

                    case let .deepLinkFeed(feedId):
                        DeepLinkFeedScreen(feedId: feedId, readerModeViewModel: readerModeViewModel)

                    case .inAppBrowser:
                        EmptyView()
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        } detail: {
            NavigationStack(path: $detailNavigationPath) {
                detailPaneView
                    .environment(appState)
                    .environment(browserSelector)
                    .navigationDestination(for: CommonViewRoute.self) { route in
                        switch route {
                        case let .inAppBrowser(url):
                            SFSafariView(url: url)
                                .ignoresSafeArea()
                                .navigationBarBackButtonHidden(true)
                        default:
                            EmptyView()
                        }
                    }
            }
        }
        .navigationSplitViewStyle(.balanced)
        .background(Color(.systemGroupedBackground))
        .sheet(isPresented: $showAddFeedSheet) {
            AddFeedScreen(showCloseButton: true)
                .environment(appState)
                .environment(browserSelector)
        }
        .sheet(isPresented: $showEditFeedSheet) {
            if let feedSource = feedSourceToEdit {
                EditFeedScreen(feedSource: feedSource)
                    .environment(appState)
                    .environment(browserSelector)
            }
        }
        .onChange(of: appState.pendingBrowserURL) { _, newURL in
            if let url = newURL {
                appState.pendingBrowserURL = nil
                detailContent = .inAppBrowser(url: url)
                detailNavigationPath = NavigationPath()
                preferredColumn = .detail
            }
        }
        .onChange(of: appState.pendingExternalURL) { _, newURL in
            if let url = newURL {
                appState.pendingExternalURL = nil
                openURL(url)
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
            detailContent = .empty
            detailNavigationPath = NavigationPath()
            preferredColumn = .content
        }
    }

    @ViewBuilder private var detailPaneView: some View {
        switch detailContent {
        case .empty:
            EmptyDetailPane()
        case .readerMode:
            ReaderModeScreen(
                viewModel: readerModeViewModel,
                onInAppBrowserClick: { url in
                    if url.scheme == "http" || url.scheme == "https" {
                        detailNavigationPath.append(CommonViewRoute.inAppBrowser(url: url))
                    } else {
                        openURL(url)
                    }
                }
            )
        case let .inAppBrowser(url):
            SFSafariView(url: url, onDismiss: {
                detailContent = .empty
                preferredColumn = .content
            })
            .id(url)
            .ignoresSafeArea()
            .toolbar(.hidden, for: .navigationBar)
        }
    }
}
