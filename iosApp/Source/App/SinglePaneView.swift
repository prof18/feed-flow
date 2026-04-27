import FeedFlowKit
import Foundation
import Reader
import SwiftUI

struct SinglePaneView: View {
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

    @State private var columnVisibility: NavigationSplitViewVisibility = .detailOnly
    @State private var preferredColumn: NavigationSplitViewColumn = .detail

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
                    appState.regularNavigationPath = NavigationPath()
                    columnVisibility = .detailOnly
                    preferredColumn = .detail
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
                        preferredColumn = .sidebar
                    },
                    onSidebarSelectionChanged: { feedFilter in
                        selectedSidebarItem = sidebarSelection(from: feedFilter)
                    }
                )
                .environment(indexHolder)
                .environment(appState)
                .environment(browserSelector)
                .navigationDestination(for: CommonViewRoute.self) { route in
                    switch route {
                    case .readerMode:
                        ReaderModeScreen(
                            viewModel: readerModeViewModel,
                            onInAppBrowserClick: { url in
                                if url.scheme == "http" || url.scheme == "https" {
                                    appState.regularNavigationPath.append(CommonViewRoute.inAppBrowser(url: url))
                                } else {
                                    openURL(url)
                                }
                            }
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
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .navigationSplitViewStyle(.prominentDetail)
        .background(Color(.systemGroupedBackground))
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
                appState.regularNavigationPath.append(CommonViewRoute.inAppBrowser(url: url))
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
            appState.regularNavigationPath = NavigationPath()
            columnVisibility = .detailOnly
            preferredColumn = .detail
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
