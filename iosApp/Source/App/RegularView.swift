//
//  RegularView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import KMPNativeCoroutinesAsync
import Reeeed

struct RegularView: View {
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
    @State var showSettings: Bool = false
    @State private var browserToOpen: BrowserToPresent?

    @State var showAddFeedSheet = false

    @StateObject var indexHolder: HomeListIndexHolder
    var drawerItems: [DrawerItem] = []
    let homeViewModel: HomeViewModel

    var body: some View {
        NavigationSplitView {
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
                }
            )
            .navigationBarTitleDisplayMode(.inline)
        } detail: {
            NavigationStack(path: $appState.regularNavigationPath) {
                HomeScreen(
                    toggleListScroll: $scrollUpTrigger,
                    showSettings: $showSettings,
                    selectedDrawerItem: $selectedDrawerItem,
                    homeViewModel: homeViewModel
                )
                .environmentObject(indexHolder)
            }
            .navigationBarTitleDisplayMode(.inline)
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
        .sheet(isPresented: $showAddFeedSheet) {
            AddFeedScreen(showCloseButton: true)
        }
        .navigationSplitViewStyle(.balanced)
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.navDrawerStateFlow)
                for try await state in stream {
                    self.navDrawerState = state
                }
            } catch {
                if !(error is CancellationError) {
                                    self.appState.emitGenericError()
                                }            }
        }
    }
}
