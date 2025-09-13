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

    @Binding var selectedDrawerItem: DrawerItem?

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
    @State var isToggled = false

    @State var indexHolder: HomeListIndexHolder
    var drawerItems: [DrawerItem] = []
    let homeViewModel: HomeViewModel

    @State private var showEditFeedSheet = false
    @State private var feedSourceToEdit: FeedSource?

    @State private var columnVisibility: NavigationSplitViewVisibility = .automatic

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            SidebarDrawer(
                selectedDrawerItem: $selectedDrawerItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    indexHolder.clear()
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
            .navigationBarTitleDisplayMode(.inline)
        } detail: {
            @Bindable var appState = appState
            NavigationStack(path: $appState.regularNavigationPath) {
                HomeScreen(
                    toggleListScroll: $scrollUpTrigger,
                    showSettings: $showSettings,
                    selectedDrawerItem: $selectedDrawerItem,
                    columnVisibility: $columnVisibility,
                    homeViewModel: homeViewModel
                ) {
                    columnVisibility = .all
                }
                .environment(indexHolder)
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: CommonViewRoute.self) { route in
                switch route {
                case let .readerMode(feedItem):
                    ReaderModeScreen(
                        feedItemUrlInfo: FeedItemUrlInfo(
                            id: feedItem.id,
                            url: feedItem.url,
                            title: feedItem.title,
                            openOnlyOnBrowser: false,
                            isBookmarked: feedItem.isBookmarked,
                            linkOpeningPreference: feedItem.feedSource.linkOpeningPreference
                        )
                    )

                case .search:
                    SearchScreen()

                case .accounts:
                    AccountsScreen()

                case .dropboxSync:
                    DropboxSyncScreen()

                case let .deepLinkFeed(feedId):
                    DeepLinkFeedScreen(feedId: feedId)

                case let .inAppBrowser(url):
                    SFSafariView(url: url)
                        .ignoresSafeArea()
                        .navigationBarBackButtonHidden(true)
                }
            }
        }
        .sheet(isPresented: $showAddFeedSheet) {
            AddFeedScreen(showCloseButton: true)
        }
        .sheet(isPresented: $showEditFeedSheet) {
            if let feedSource = feedSourceToEdit {
                EditFeedScreen(feedSource: feedSource)
            }
        }
        .navigationSplitViewStyle(.balanced)
        .task {
            for await state in homeViewModel.navDrawerState {
                self.navDrawerState = state
            }
        }
    }
}
