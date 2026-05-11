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

struct RegularView: View {
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
    @State private var showSidebarSettingsSheet = false
    @State var showAddFeedSheet = false
    @State private var showImportExportSheet = false

    @State var indexHolder: HomeListIndexHolder
    let homeViewModel: HomeViewModel
    let readerModeViewModel: ReaderModeViewModel

    @State private var showEditFeedSheet = false
    @State private var feedSourceToEdit: FeedSource?
    @State private var columnVisibility: NavigationSplitViewVisibility = .automatic

    @State private var showFeedOperationDialog = false
    @State private var feedOperationLoadingMessage: String?

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            SidebarDrawer(
                selectedSidebarItem: $selectedSidebarItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    indexHolder.clear()
                    scrollUpTrigger.toggle()
                    appState.currentCommonRoute = nil
                    appState.regularNavigationPath = NavigationPath()
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
                    showSidebarSettingsSheet.toggle()
                },
                onAddFeedClick: {
                    showAddFeedSheet.toggle()
                },
                onImportExportClick: {
                    showImportExportSheet.toggle()
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
                },
                validateCategoryName: { categoryId, categoryName in
                    homeViewModel.validateCategoryName(
                        categoryId: CategoryId(value: categoryId),
                        newName: categoryName
                    )
                }
            )
            .environment(appState)
            .environment(browserSelector)
            .navigationBarTitleDisplayMode(.inline)
        } detail: {
            @Bindable var appState = appState
            NavigationStack(path: $appState.regularNavigationPath) {
                HomeScreen(
                    toggleListScroll: $scrollUpTrigger,
                    showSettings: $showSettings,
                    columnVisibility: $columnVisibility,
                    homeViewModel: homeViewModel,
                    readerModeViewModel: readerModeViewModel,
                    onReaderModeNavigate: nil,
                    openDrawer: {
                        columnVisibility = .all
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
                .navigationDestination(for: CommonViewRoute.self) { route in
                    commonDestination(route)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .navigationSplitViewStyle(.balanced)
        .background(Color(.systemGroupedBackground))
        .loadingDialog(isLoading: showFeedOperationDialog, message: feedOperationLoadingMessage)
        .task {
            for await state in homeViewModel.feedOperationState {
                switch onEnum(of: state) {
                case .none:
                    self.feedOperationLoadingMessage = nil
                    self.showFeedOperationDialog = false
                case .deleting:
                    self.feedOperationLoadingMessage = feedFlowStrings.deletingFeedDialogTitle
                    self.showFeedOperationDialog = true
                case .markingAllRead:
                    self.feedOperationLoadingMessage = feedFlowStrings.markingAllReadDialogTitle
                    self.showFeedOperationDialog = true
                }
            }
        }
        .sheet(isPresented: $showAddFeedSheet) {
            AddFeedScreen(showCloseButton: true)
                .environment(appState)
                .environment(browserSelector)
                .toggleStyle(BlueToggleStyle())
        }
        .sheet(isPresented: $showSidebarSettingsSheet) {
            SettingsScreen(fetchFeeds: { homeViewModel.forceFeedRefresh() })
                .environment(appState)
                .preferredColorScheme(appState.colorScheme)
        }
        .sheet(isPresented: $showImportExportSheet) {
            ImportExportScreen(showCloseButton: true, fetchFeeds: { homeViewModel.forceFeedRefresh() })
                .environment(appState)
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
                appState.regularNavigationPath.append(CommonViewRoute.inAppBrowser(url: url))
            }
        }
        .onChange(of: appState.pendingExternalURL) { _, newURL in
            if let url = newURL {
                appState.pendingExternalURL = nil
                openURL(url)
            }
        }
        .onChange(of: appState.regularNavigationPath.count) { _, newCount in
            if newCount == 0 {
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
            showSidebarSettingsSheet = false
            appState.currentCommonRoute = nil
            appState.regularNavigationPath = NavigationPath()
        }
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
