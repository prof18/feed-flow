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
    @Environment(\.horizontalSizeClass)
    private var horizontalSizeClass

    @Binding var selectedSidebarItem: SidebarSelection?

    @State private var navDrawerState: NavDrawerState = .init(
        timeline: [],
        read: [],
        bookmarks: [],
        categories: [],
        pinnedFeedSources: [],
        feedSourcesWithoutCategory: [],
        feedSourcesByCategory: [:],
        uncategorizedPosition: 0
    )
    @State private var scrollUpTrigger = false
    @State private var showSettings = false
    @State private var showSidebarSettingsSheet = false
    @State private var showAddFeedSheet = false
    @State private var showImportExportSheet = false

    @State private var indexHolder: HomeListIndexHolder
    let homeViewModel: HomeViewModel
    let readerModeViewModel: ReaderModeViewModel
    let onOpenAudioEpisode: (AudioEpisode) -> Void

    @State private var feedSourceToEdit: EditFeedSheetItem?
    @State private var columnVisibility: NavigationSplitViewVisibility
    @State private var preferredCompactColumn: NavigationSplitViewColumn = .detail

    @State private var showFeedOperationDialog = false
    @State private var feedOperationLoadingMessage: String?

    init(
        selectedSidebarItem: Binding<SidebarSelection?>,
        homeViewModel: HomeViewModel,
        readerModeViewModel: ReaderModeViewModel,
        onOpenAudioEpisode: @escaping (AudioEpisode) -> Void
    ) {
        _selectedSidebarItem = selectedSidebarItem
        _indexHolder = State(initialValue: HomeListIndexHolder(homeViewModel: homeViewModel))
        _columnVisibility = State(initialValue: Self.savedColumnVisibility(homeViewModel: homeViewModel))
        self.homeViewModel = homeViewModel
        self.readerModeViewModel = readerModeViewModel
        self.onOpenAudioEpisode = onOpenAudioEpisode
    }

    var body: some View {
        NavigationSplitView(
            columnVisibility: $columnVisibility,
            preferredCompactColumn: $preferredCompactColumn
        ) {
            SidebarDrawer(
                selectedSidebarItem: $selectedSidebarItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    indexHolder.clear()
                    scrollUpTrigger.toggle()
                    appState.regularNavigationPath = NavigationPath()
                    preferredCompactColumn = .detail
                    homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
                },
                onMarkAllReadClick: {
                    homeViewModel.markAllRead()
                },
                onDeleteOldFeedClick: {
                    homeViewModel.deleteOldFeedItems()
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
                    feedSourceToEdit = EditFeedSheetItem(feedSource: feedSource)
                },
                onDeleteFeedClick: { feedSource in
                    homeViewModel.deleteFeedSource(feedSource: feedSource)
                },
                onPinFeedClick: { feedSource in
                    homeViewModel.toggleFeedPin(feedSource: feedSource)
                },
                onMarkAllReadForFeedSource: { feedSource in
                    homeViewModel.markAllReadForFeedSource(feedSource: feedSource)
                },
                onMarkAllReadForCategory: { category in
                    homeViewModel.markAllReadForCategory(category: category)
                },
                onDeleteAllFeedsInCategory: { categoryId in
                    homeViewModel.deleteAllFeedsInCategory(categoryId: CategoryId(value: categoryId))
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
                },
                onReorderPinnedFeedSources: { feedSources in
                    homeViewModel.reorderPinnedFeedSources(feedSources: feedSources)
                },
                onReorderCategories: { categoryWrappers in
                    homeViewModel.reorderCategories(categoryWrappers: categoryWrappers)
                },
                onReorderFeedSources: { feedSources in
                    homeViewModel.reorderFeedSources(feedSources: feedSources)
                }
            )
            .modifier(NowPlayingAccessoryModifier(
                isEnabled: horizontalSizeClass == .compact,
                onOpenEpisode: onOpenAudioEpisode
            ))
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
                        preferredCompactColumn = .sidebar
                    },
                    onSidebarSelectionChanged: { feedFilter in
                        selectedSidebarItem = sidebarSelection(from: feedFilter)
                    }
                )
                .modifier(NowPlayingAccessoryModifier(onOpenEpisode: onOpenAudioEpisode))
                .environment(indexHolder)
                .environment(appState)
                .environment(browserSelector)
                .navigationDestination(for: CommonViewRoute.self) { route in
                    commonDestination(route)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .navigationSplitViewStyle(.balanced)
        .onChange(of: horizontalSizeClass) { oldSizeClass, newSizeClass in
            if oldSizeClass == .regular, newSizeClass == .compact {
                preferredCompactColumn = .detail
            } else if oldSizeClass == .compact, newSizeClass == .regular {
                // The compact layout (a folded Duo) resets the split view, so bring back the saved choice.
                columnVisibility = Self.savedColumnVisibility(homeViewModel: homeViewModel)
            }
        }
        .onChange(of: columnVisibility) { _, visibility in
            guard horizontalSizeClass != .compact else { return }
            if visibility == .detailOnly {
                homeViewModel.setLargeScreenSidebarVisible(value: false)
            } else if visibility == .all || visibility == .doubleColumn {
                homeViewModel.setLargeScreenSidebarVisible(value: true)
            }
        }
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
            SettingsScreen(
                fetchFeeds: { homeViewModel.getNewFeeds(isFirstLaunch: false, forceRefresh: false) },
                onDone: { homeViewModel.reloadFeedState() }
            )
                .environment(appState)
                .environment(browserSelector)
                .preferredColorScheme(appState.colorScheme)
        }
        .sheet(isPresented: $showImportExportSheet) {
            ImportExportScreen(
                showCloseButton: true,
                fetchFeeds: { homeViewModel.getNewFeeds(isFirstLaunch: false, forceRefresh: false) }
            )
                .environment(appState)
        }
        .sheet(item: $feedSourceToEdit) { item in
            EditFeedScreen(feedSource: item.feedSource)
                .environment(appState)
                .environment(browserSelector)
                .toggleStyle(BlueToggleStyle())
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
        .onChange(of: appState.regularNavigationPath.count) { _, newCount in
            if newCount > 0 {
                preferredCompactColumn = .detail
            }
        }
        .task {
            for await state in homeViewModel.navDrawerState {
                self.navDrawerState = state
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .didReceiveNotificationDeepLink)) { _ in
            showAddFeedSheet = false
            feedSourceToEdit = nil
            showSettings = false
            showSidebarSettingsSheet = false
            appState.regularNavigationPath = NavigationPath()
            preferredCompactColumn = .detail
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
        .modifier(NowPlayingAccessoryModifier(route: route, onOpenEpisode: onOpenAudioEpisode))
        .environment(appState)
        .environment(browserSelector)
    }

    private static func savedColumnVisibility(homeViewModel: HomeViewModel) -> NavigationSplitViewVisibility {
        homeViewModel.getLargeScreenSidebarVisible().map {
            $0.boolValue ? .all : .detailOnly
        } ?? .automatic
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
