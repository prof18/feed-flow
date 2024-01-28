//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 15/01/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import shared

struct HomeContent: View {

    @EnvironmentObject private var indexHolder: HomeListIndexHolder
    @EnvironmentObject private var appState: AppState

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    @Binding var loadingState: FeedUpdateStatus?
    @Binding var feedState: [FeedItem]
    @Binding var showLoading: Bool
    @Binding var unreadCount: Int
    @Binding var sheetToShow: HomeSheetToShow?
    @Binding var toggleListScroll: Bool
    @Binding var currentFeedFilter: FeedFilter

    let onRefresh: () -> Void
    let updateReadStatus: (Int32) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let onForceRefreshClick: () -> Void
    let deleteAllFeeds: () -> Void
    let requestNewPage: () -> Void
    let onItemClick: (FeedItemClickedInfo) -> Void

    var body: some View {
        ScrollViewReader { proxy in
            FeedListView(
                loadingState: loadingState,
                feedState: feedState,
                showLoading: showLoading,
                onReloadClick: onRefresh,
                onAddFeedClick: {
                    self.sheetToShow = .noFeedSource
                },
                requestNewPage: requestNewPage,
                onItemClick: onItemClick
            )
            .onChange(of: toggleListScroll) { _ in
                proxy.scrollTo(feedState.first?.id)
            }
            .if(appState.sizeClass == .compact) { view in
                view.navigationBarBackButtonHidden(true)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                makeToolbarHeaderView(proxy: proxy)
                makeMenuToolbarView(proxy: proxy)
            }

        }
        .sheet(item: $sheetToShow) { item in
            switch item {
            case .settings:
                SettingsScreen()

            case .noFeedSource:
                NoFeedsBottomSheet(
                    onAddFeedClick: {
                        self.sheetToShow = .addFeed
                    },
                    onImportExportClick: {
                        self.sheetToShow = .importExport
                    }
                )
            case .addFeed:
                AddFeedScreen(showCloseButton: true)

            case .importExport:
                ImportExportScreen(showCloseButton: true)
            }
        }
    }

    @ToolbarContentBuilder
    private func makeToolbarHeaderView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(id: UUID().uuidString, placement: .navigationBarLeading, showsByDefault: true) {
            HStack {
                if appState.sizeClass == .compact {
                    Button {
                        self.dismiss()
                    } label: {
                        Image(systemName: "sidebar.left")
                    }
                    .accessibilityIdentifier(TestingTag.shared.DRAWER_MENU_BUTTON)
                }

                HStack {
                    Text(currentFeedFilter.getNavBarName())
                        .font(.title2)

                    if !(currentFeedFilter is FeedFilter.Read) {
                        Text("(\(unreadCount))")
                            .font(.title2)
                    }
                }
                .padding(.vertical, Spacing.medium)
                .onTapGesture(count: 2) {
                    onRefresh()
                    proxy.scrollTo(feedState.first?.id)
                    updateReadStatus(Int32(indexHolder.getLastReadIndex()))
                    self.indexHolder.refresh()
                }
                .accessibilityIdentifier(TestingTag.shared.HOME_TOOLBAR)
            }
        }
    }

    private func makeMenuToolbarView(proxy: ScrollViewProxy) -> some ToolbarContent {
        ToolbarItem(id: UUID().uuidString, placement: .primaryAction, showsByDefault: true) {
            Menu {
                Button {
                    onMarkAllReadClick()
                } label: {
                    Label(localizer.mark_all_read_button.localized, systemImage: "checkmark")
                }

                Button {
                    onDeleteOldFeedClick()
                } label: {
                    Label(localizer.clear_old_articles_button.localized, systemImage: "trash")
                }

                Button {
                    proxy.scrollTo(feedState.first?.id)
                    onForceRefreshClick()
                } label: {
                    Label(localizer.force_feed_refresh.localized, systemImage: "arrow.clockwise")
                }

                Button {
                    self.sheetToShow = .settings
                } label: {
                    Label(localizer.settings_button.localized, systemImage: "gear")
                }
                .accessibilityLabel(TestingTag.shared.SETTINGS_MENU)

                #if DEBUG
                Button {
                    deleteAllFeeds()
                } label: {
                    Label("Delete Database", systemImage: "trash")
                }
                #endif

            } label: {
                Image(systemName: "ellipsis.circle")
            }
            .accessibilityIdentifier(TestingTag.shared.SETTING_BUTTON)
        }
    }
}

fileprivate extension FeedFilter {
    func getNavBarName() -> String {
        switch self {
        case let category as FeedFilter.Category:
            return category.feedCategory.title

        case let source as FeedFilter.Source:
            return source.feedSource.title

        case is FeedFilter.Read:
            return localizer.drawer_title_read.localized

        default:
            return localizer.app_name.localized
        }
    }
}

#Preview("HomeContentLoading") {
    HomeContent(
        loadingState: .constant(
            InProgressFeedUpdateStatus(
                refreshedFeedCount: Int32(10),
                totalFeedCount: Int32(42)
            )
        ),
        feedState: .constant(PreviewItemsKt.feedItemsForPreview),
        showLoading: .constant(true),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        onRefresh: { },
        updateReadStatus: { _ in },
        onMarkAllReadClick: { },
        onDeleteOldFeedClick: { },
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in }
    )
    .environmentObject(HomeListIndexHolder())
    .environmentObject(AppState())
    .environmentObject(BrowserSelector())
}

#Preview("HomeContentLoaded") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(PreviewItemsKt.feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        onRefresh: { },
        updateReadStatus: { _ in },
        onMarkAllReadClick: { },
        onDeleteOldFeedClick: { },
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in }
    )
    .environmentObject(HomeListIndexHolder())
    .environmentObject(AppState())
    .environmentObject(BrowserSelector())
}

#Preview("HomeContentSettings") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(PreviewItemsKt.feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(HomeSheetToShow.noFeedSource),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        onRefresh: { },
        updateReadStatus: { _ in },
        onMarkAllReadClick: { },
        onDeleteOldFeedClick: { },
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in }
    )
    .environmentObject(HomeListIndexHolder())
    .environmentObject(AppState())
    .environmentObject(BrowserSelector())
}
