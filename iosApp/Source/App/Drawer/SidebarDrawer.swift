//
//  SidebarDrawer.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import NukeUI
import SwiftUI

@MainActor
struct SidebarDrawer: View {
    @Environment(AppState.self) private var appState
    @Binding var selectedDrawerItem: DrawerItem?

    let navDrawerState: NavDrawerState
    let onFeedFilterSelected: (FeedFilter) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let onForceRefreshClick: () -> Void
    let deleteAllFeeds: () -> Void
    let onShowSettingsClick: () -> Void
    let onAddFeedClick: () -> Void
    let onEditFeedClick: (FeedSource) -> Void
    let onDeleteFeedClick: (FeedSource) -> Void
    let onPinFeedClick: (FeedSource) -> Void
    let onDeleteCategory: (String) -> Void
    let onUpdateCategoryName: (String, String) -> Void

    var body: some View {
        List(selection: $selectedDrawerItem) {
            TimelineSection(
                timeline: navDrawerState.timeline,
                onSelect: { self.selectedDrawerItem = $0 },
                onFeedFilterSelected: onFeedFilterSelected
            )

            ReadSection(
                read: navDrawerState.read,
                onSelect: { self.selectedDrawerItem = $0 },
                onFeedFilterSelected: onFeedFilterSelected
            )

            BookmarksSection(
                bookmarks: navDrawerState.bookmarks,
                onSelect: { self.selectedDrawerItem = $0 },
                onFeedFilterSelected: onFeedFilterSelected
            )

            if !navDrawerState.pinnedFeedSources.isEmpty {
                pinnedFeedSourcesSection
            }

            CategoriesSection(
                categories: navDrawerState.categories,
                onSelect: { self.selectedDrawerItem = $0 },
                onFeedFilterSelected: onFeedFilterSelected,
                onDeleteCategory: onDeleteCategory,
                onUpdateCategoryName: onUpdateCategoryName
            )

            feedSourcesWithoutCategorySection
            feedSourcesWithCategorySection

            if isOnVisionOSDevice() {
                visionOsSection
            }
        }
        .listStyle(.sidebar)
    }

    private var pinnedFeedSourcesSection: some View {
        Section(
            content: {
                ForEach(navDrawerState.pinnedFeedSources, id: \.self) { drawerItem in
                    if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                        makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
                    }
                }
            },
            header: {
                Text(feedFlowStrings.drawerTitlePinnedFeeds)
            }
        )
    }

    @ViewBuilder
    private var feedSourcesWithoutCategorySection: some View {
        if !navDrawerState.feedSourcesWithoutCategory.isEmpty {
            Section(
                content: {
                    ForEach(navDrawerState.feedSourcesWithoutCategory, id: \.self) { drawerItem in
                        if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                            makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
                        }
                    }
                },
                header: {
                    makeAddFeedButton(title: feedFlowStrings.drawerTitleFeedSources)
                }
            )
        }
    }

    @ViewBuilder
    private var feedSourcesWithCategorySection: some View {
        if !navDrawerState.feedSourcesByCategory.isEmpty {
            Section(
                content: {
                    ForEach(
                        navDrawerState.feedSourcesByCategory.keys.sorted {
                            $0.feedSourceCategory?.title ?? "" < $1.feedSourceCategory?.title ?? ""
                        },
                        id: \.self
                    ) { category in
                        let categoryWrapper =
                            category as DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper

                        let title = categoryWrapper.feedSourceCategory?.title ?? feedFlowStrings.noCategory
                        makeCategoryDropdown(
                            drawerItems: navDrawerState.feedSourcesByCategory[categoryWrapper] ?? [],
                            title: title
                        ).accessibilityIdentifier("\(TestingTag.shared.FEED_SOURCE_SELECTOR)_\(title)")
                    }
                },
                header: {
                    makeAddFeedButton(title: feedFlowStrings.drawerTitleFeedSources)
                }
            )
        }
    }

    @ViewBuilder
    private func makeAddFeedButton(title: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            Button(
                action: {
                    onAddFeedClick()
                },
                label: {
                    Image(systemName: "plus.app")
                }
            )
        }
        .if(appState.sizeClass == .compact) { view in
            view.listRowInsets(
                EdgeInsets(
                    top: Spacing.small,
                    leading: Spacing.small,
                    bottom: Spacing.small,
                    trailing: Spacing.small
                )
            )
        }.if(appState.sizeClass == .regular) { view in
            view.listRowInsets(
                EdgeInsets(
                    top: Spacing.small,
                    leading: Spacing.small,
                    bottom: Spacing.small,
                    trailing: -Spacing.xsmall
                )
            )
        }
    }

    @ViewBuilder
    private func makeFeedSourceDrawerItem(drawerItem: DrawerItem.DrawerFeedSource) -> some View {
        FeedSourceDrawerItem(
            drawerItem: drawerItem,
            onSelect: { item in
                self.selectedDrawerItem = item
                self.onFeedFilterSelected(FeedFilter.Source(feedSource: item.feedSource))
            },
            onEdit: onEditFeedClick,
            onPin: onPinFeedClick,
            onDelete: onDeleteFeedClick
        )
    }

    @ViewBuilder
    private func makeCategoryDropdown(drawerItems: [DrawerItem], title: String) -> some View {
        DisclosureGroup(
            content: {
                ForEach(drawerItems, id: \.self) { drawerItem in
                    if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                        makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
                            .listRowInsets(
                                EdgeInsets(
                                    top: Spacing.small,
                                    leading: .zero,
                                    bottom: Spacing.small,
                                    trailing: Spacing.small
                                )
                            )
                    } else {
                        EmptyView()
                    }
                }
            },
            label: {
                Text(title)
            }
        )
    }

    @ViewBuilder
    private var visionOsSection: some View {
        Spacer()

        Divider()

        Button {
            onMarkAllReadClick()
        } label: {
            Label(feedFlowStrings.markAllReadButton, systemImage: "checkmark")
        }

        Button {
            onDeleteOldFeedClick()
        } label: {
            Label(feedFlowStrings.clearOldArticlesButton, systemImage: "trash")
        }

        Button {
            onForceRefreshClick()
        } label: {
            Label(feedFlowStrings.forceFeedRefresh, systemImage: "arrow.clockwise")
        }

        #if DEBUG
            Button {
                deleteAllFeeds()
            } label: {
                Label("Delete Database", systemImage: "trash")
            }
        #endif

        Button {
            onShowSettingsClick()
        } label: {
            Label(feedFlowStrings.settingsButton, systemImage: "gear")
        }
    }
}

#Preview {
    SidebarDrawer(
        selectedDrawerItem: .constant(nil),
        navDrawerState: navDrawerState,
        onFeedFilterSelected: { _ in },
        onMarkAllReadClick: {},
        onDeleteOldFeedClick: {},
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        onShowSettingsClick: {},
        onAddFeedClick: {},
        onEditFeedClick: { _ in },
        onDeleteFeedClick: { _ in },
        onPinFeedClick: { _ in },
        onDeleteCategory: { _ in },
        onUpdateCategoryName: { _, _ in }
    )
}
