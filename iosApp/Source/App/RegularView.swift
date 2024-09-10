//
//  RegularView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import FeedFlowKit
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
    @State var showAddFeedSheet = false
    @State var isToggled: Bool = false

    @State private var showFontSizeMenu: Bool = false
    @State private var fontSize = 16.0
    @State private var isSliderMoving = false
    @State private var reset = false

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(Deps.shared.getReaderModeViewModel())

    @State private var browserToOpen: BrowserToPresent?
    @State var indexHolder: HomeListIndexHolder
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
                .environment(indexHolder)
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: CommonViewRoute.self) { route in
                switch route {
                case .readerMode(let url):
                    ReeeederView(
                        url: url,
                        options: ReeeederViewOptions(
                            theme: .init(
                                additionalCSS: """
                                    #__reader_container {
                                        font-size: \(fontSize)px
                                    }
                                """
                            ),
                            onLinkClicked: { url in
                                if browserSelector.openInAppBrowser() {
                                    browserToOpen = .inAppBrowser(url: url)
                                } else {
                                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url.absoluteString))
                                }
                            }
                        ),
                        toolbarContent: {
                            Button {
                                if browserSelector.openInAppBrowser() {
                                    browserToOpen = .inAppBrowser(url: url)
                                } else {
                                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url.absoluteString))
                                }
                            } label: {
                                Image(systemName: "globe")
                            }

                            ShareLink(item: url) {
                                Label("Share", systemImage: "square.and.arrow.up")
                            }
                            fontSizeMenu
                        }
                    )
                    .id(reset)

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
            for await state in homeViewModel.navDrawerState {
                self.navDrawerState = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.readerFontSizeState {
                self.fontSize = Double(truncating: state)
            }
        }
    }

    @ViewBuilder
    private var fontSizeMenu: some View {
        Button {
            showFontSizeMenu.toggle()
        } label: {
            Image(systemName: "textformat.size")
        }
        .font(.title3)
        .popover(isPresented: $showFontSizeMenu) {
            VStack(alignment: .leading) {
                Text(feedFlowStrings.readerModeFontSize)

                HStack {
                    Button {
                        fontSize -= 1.0
                        self.reset.toggle()
                        vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                    } label: {
                        Image(systemName: "minus")
                    }

                    Slider(
                        value: $fontSize,
                        in: 12...40,
                        onEditingChanged: { isEditing in
                            if !isEditing {
                                self.reset.toggle()
                                vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                            }
                        }
                    )

                    Button {
                        fontSize += 1.0
                        self.reset.toggle()
                        vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .frame(width: 250, height: 100)
            .padding(.horizontal, Spacing.regular)
            .presentationCompactAdaptation((.popover))

        }
    }
}
