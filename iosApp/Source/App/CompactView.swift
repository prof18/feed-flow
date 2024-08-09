//
//  CompactView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import Reeeed

struct CompactView: View {

    @EnvironmentObject var appState: AppState

    @EnvironmentObject private var browserSelector: BrowserSelector

    @Environment(\.openURL) private var openURL

    @Binding var selectedDrawerItem: DrawerItem?

    @State var navDrawerState: NavDrawerState = NavDrawerState(
        timeline: [],
        read: [],
        bookmarks: [],
        categories: [],
        feedSourcesWithoutCategory: [],
        feedSourcesByCategory: [:]
    )
    @State var scrollUpTrigger: Bool = false

    @State private var browserToOpen: BrowserToPresent?

    @State var showAddFeedSheet = false

    @StateObject var indexHolder: HomeListIndexHolder
    let homeViewModel: HomeViewModel

    var body: some View {
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
                }
            ).sheet(isPresented: $showAddFeedSheet) {
                AddFeedScreen(showCloseButton: true)
            }
            .navigationDestination(for: CompactViewRoute.self) { route in
                switch route {
                case .feed:
                    HomeScreen(
                        toggleListScroll: $scrollUpTrigger,
                        showSettings: .constant(false),
                        selectedDrawerItem: $selectedDrawerItem,
                        homeViewModel: homeViewModel
                    )
                    .environmentObject(indexHolder)
                }
            }
            .navigationDestination(for: CommonViewRoute.self) { route in
                switch route {
                case .readerMode(let url):
                    ReeeederView(
                        url: url,
                        options: ReeeederViewOptions(
                            theme: ReaderTheme(),
                            onLinkClicked: { url in
                                if browserSelector.openInAppBrowser() {
                                    browserToOpen = .inAppBrowser(url: url)
                                } else {
                                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url.absoluteString))
                                }
                            }
                        )
                    )

                case .search:
                    SearchScreen()

                case .accounts:
                    AccountsScreen()

                case .dropboxSync:
                    DropboxSyncScreen()
                }
            }
            .fullScreenCover(item: $browserToOpen) { browserToOpen in
                switch browserToOpen {
                case .inAppBrowser(let url):
                    SFSafariView(url: url)
                        .ignoresSafeArea()
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
