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
    @State var showAddFeedSheet = false
    @State var showEditFeedSheet = false

    @State var indexHolder: HomeListIndexHolder
    let homeViewModel: HomeViewModel

    @State private var feedSourceToEdit: FeedSource?

    var body: some View {
        @Bindable var appState = appState
        NavigationStack(path: $appState.compatNavigationPath) {
            SidebarDrawer(
                selectedDrawerItem: $selectedDrawerItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    indexHolder.clear()
                    appState.navigate(route: CompactViewRoute.feed)
                    scrollUpTrigger.toggle()
                    homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
                },
                onMarkAllReadClick: {
                    // On compact view it's handled by the home
                },
                onDeleteOldFeedClick: {
                    // On compact view it's handled by the home
                },
                onForceRefreshClick: {
                    // On compact view it's handled by the home
                },
                deleteAllFeeds: {
                    // On compact view it's handled by the home
                },
                onShowSettingsClick: {
                    // On compact view it's handled by the home
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
            ).sheet(isPresented: $showAddFeedSheet) {
                AddFeedScreen(showCloseButton: true)
            }
            .sheet(isPresented: $showEditFeedSheet) {
                if let feedSource = feedSourceToEdit {
                    EditFeedScreen(feedSource: feedSource)
                }
            }
            .navigationDestination(for: CompactViewRoute.self) { route in
                switch route {
                case .feed:
                    HomeScreen(
                        toggleListScroll: $scrollUpTrigger,
                        showSettings: .constant(false),
                        selectedDrawerItem: $selectedDrawerItem,
                        columnVisibility: .constant(.automatic),
                        homeViewModel: homeViewModel
                    ) {
                        // Handle by the view for the compact view
                    }
                    .environment(indexHolder)
                }
            }
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
        .navigationBarTitleDisplayMode(.inline)
        .task {
            for await state in homeViewModel.navDrawerState {
                self.navDrawerState = state
            }
        }
    }
}
