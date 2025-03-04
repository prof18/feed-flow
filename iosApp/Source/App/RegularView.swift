//
//  RegularView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import Reeeed
import SwiftUI

struct RegularView: View {
    @Environment(AppState.self) private var appState
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(\.openURL) private var openURL

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
    @State var scrollUpTrigger: Bool = false
    @State var showSettings: Bool = false
    @State var showAddFeedSheet = false
    @State var isToggled: Bool = false

    @State private var showFontSizeMenu: Bool = false
    @State private var fontSize = 16.0
    @State private var isSliderMoving = false
    @State private var reset = false
    @State private var isBookmarked = false

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    @State private var browserToOpen: BrowserToPresent?
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
                    homeViewModel: homeViewModel,
                    openDrawer: {
                        columnVisibility = .all
                    }
                )
                .environment(indexHolder)
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: CommonViewRoute.self) { route in
                switch route {
                case let .readerMode(feedItem):
                    Group {
                        ReeeederView(
                            url: URL(string: feedItem.url)!,
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
                                    isBookmarked.toggle()
                                    vmStoreOwner.instance.updateBookmarkStatus(
                                        feedItemId: FeedItemId(id: feedItem.id),
                                        bookmarked: isBookmarked
                                    )
                                } label: {
                                    if isBookmarked {
                                        Image(systemName: "bookmark.slash")
                                    } else {
                                        Image(systemName: "bookmark")
                                    }
                                }

                                ShareLink(item: URL(string: feedItem.url)!) {
                                    Label("Share", systemImage: "square.and.arrow.up")
                                }

                                Button {
                                    if browserSelector.openInAppBrowser() {
                                        browserToOpen = .inAppBrowser(url: URL(string: feedItem.url)!)
                                    } else {
                                        openURL(
                                            browserSelector.getUrlForDefaultBrowser(
                                                stringUrl: URL(string: feedItem.url)!.absoluteString))
                                    }
                                } label: {
                                    Image(systemName: "globe")
                                }

                                fontSizeMenu
                            }
                        )
                        .onAppear {
                            isBookmarked = feedItem.isBookmarked
                        }
                        .id(reset)
                    }

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
                case let .inAppBrowser(url):
                    SFSafariView(url: url)
                        .ignoresSafeArea()
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
                        in: 12 ... 40,
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
