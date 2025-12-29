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
    @Environment(AppState.self)
    private var appState

    @Environment(BrowserSelector.self)
    private var browserSelector: BrowserSelector

    @Environment(\.openURL)
    private var openURL

    @Binding var selectedDrawerItem: DrawerItem?

    @StateObject private var categoryVMStoreOwner = VMStoreOwner<ChangeFeedCategoryViewModel>(
        Deps.shared.getChangeFeedCategoryViewModel()
    )

    @State private var showMarkAllReadDialog = false
    @State private var showClearOldArticlesDialog = false
    @State private var expandedCategoryIds: Set<String> = []
    @State private var showDeleteCategoryDialog = false
    @State private var showEditCategoryDialog = false
    @State private var categoryToDelete: String?
    @State private var categoryToEdit: String?
    @State private var editedCategoryName: String = ""
    @State private var showChangeCategorySheet = false
    @State private var selectedFeedForCategoryChange: FeedSource?

    let navDrawerState: NavDrawerState
    let onFeedFilterSelected: (FeedFilter) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let onForceRefreshClick: () -> Void
    let deleteAllFeeds: () -> Void
    let onShowSettingsClick: () -> Void
    let onAddFeedClick: () -> Void
    let onFeedSuggestionsClick: () -> Void
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

            if FeatureFlags.shared.ENABLE_FEED_SUGGESTIONS {
                FeedSuggestionsSection(onFeedSuggestionsClick: onFeedSuggestionsClick)
            }

            if !navDrawerState.pinnedFeedSources.isEmpty {
                pinnedFeedSourcesSection
            }

            feedSourcesSection
        }
        .listStyle(.sidebar)
        .alert(feedFlowStrings.markAllReadButton, isPresented: $showMarkAllReadDialog) {
            Button(feedFlowStrings.cancelButton, role: .cancel) {}
            Button(feedFlowStrings.confirmButton) {
                onMarkAllReadClick()
            }
        } message: {
            Text(feedFlowStrings.markAllReadDialogMessage)
        }
        .alert(feedFlowStrings.clearOldArticlesButton, isPresented: $showClearOldArticlesDialog) {
            Button(feedFlowStrings.cancelButton, role: .cancel) {}
            Button(feedFlowStrings.confirmButton) {
                onDeleteOldFeedClick()
            }
        } message: {
            Text(feedFlowStrings.clearOldArticlesDialogMessage)
        }
        .background(
            DeleteCategoryDialog(
                isPresented: $showDeleteCategoryDialog,
                categoryToDelete: $categoryToDelete,
                onDelete: onDeleteCategory
            )
        )
        .background(
            EditCategoryDialog(
                isPresented: $showEditCategoryDialog,
                categoryToEdit: $categoryToEdit,
                editedCategoryName: $editedCategoryName,
                onSave: onUpdateCategoryName
            )
        )
        .sheet(isPresented: $showChangeCategorySheet) {
            EditCategorySheetContainerForChangeCategory(
                viewModel: categoryVMStoreOwner.instance,
                onSave: {
                    categoryVMStoreOwner.instance.saveCategory()
                }
            )
        }
        .task {
            for await _ in categoryVMStoreOwner.instance.categoryChangedState {
                showChangeCategorySheet = false
                selectedFeedForCategoryChange = nil
            }
        }
    }
}

private extension SidebarDrawer {
    var pinnedFeedSourcesSection: some View {
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

    @ViewBuilder var feedSourcesSection: some View {
        if !navDrawerState.feedSourcesByCategory.isEmpty || !navDrawerState.feedSourcesWithoutCategory.isEmpty {
            let uncategorizedFromMap = navDrawerState.feedSourcesByCategory
                .filter { $0.key.feedSourceCategory == nil }
                .flatMap { $0.value }
            let uncategorizedItems: [DrawerItem] =
                Array(navDrawerState.feedSourcesWithoutCategory) + uncategorizedFromMap

            Section(
                content: {
                    makeUncategorizedDropdown(drawerItems: uncategorizedItems)

                    ForEach(
                        navDrawerState.feedSourcesByCategory.keys.sorted {
                            $0.feedSourceCategory?.title ?? "" < $1.feedSourceCategory?.title ?? ""
                        },
                        id: \.self
                    ) { category in
                        let categoryWrapper =
                            category as DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper

                        if categoryWrapper.feedSourceCategory != nil {
                            let drawerItems = navDrawerState.feedSourcesByCategory[categoryWrapper] ?? []
                            makeCategoryDropdown(
                                drawerItems: drawerItems,
                                categoryWrapper: categoryWrapper
                            )
                        } else {
                            EmptyView()
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
    func makeAddFeedButton(title: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            if #available(iOS 26.0, *) {
                Button(
                    action: {
                        onAddFeedClick()
                    },
                    label: {
                        Image(systemName: "plus")
                            .foregroundStyle(.primary)
                            .fontWeight(.medium)
                            .imageScale(.medium)
                    }
                )
                .buttonStyle(.glass)
                .modifier(GlassEffectModifier())
                .padding(.horizontal, -8)
            } else {
                Button(
                    action: {
                        onAddFeedClick()
                    },
                    label: {
                        Image(systemName: "plus.app")
                    }
                )
            }
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
        }
        .if(appState.sizeClass == .regular) { view in
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
    func makeFeedSourceDrawerItem(drawerItem: DrawerItem.DrawerFeedSource) -> some View {
        FeedSourceDrawerItem(
            drawerItem: drawerItem,
            onSelect: { item in
                self.selectedDrawerItem = item
                self.onFeedFilterSelected(FeedFilter.Source(feedSource: item.feedSource))
            },
            onEdit: onEditFeedClick,
            onPin: onPinFeedClick,
            onChangeCategory: { feedSource in
                selectedFeedForCategoryChange = feedSource
                categoryVMStoreOwner.instance.loadFeedSource(feedSource: feedSource)
                showChangeCategorySheet = true
            },
            onDelete: onDeleteFeedClick,
            onOpenWebsite: { url in
                // TODO: open in app?
                if browserSelector.openInAppBrowser() {
                    guard let url = URL(string: url) else { return }
                    self.appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                } else {
                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url))
                }
            }
        )
        .tag(drawerItem)
    }

    @ViewBuilder
    func makeCategoryDropdown(
        drawerItems: [DrawerItem],
        categoryWrapper: DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper
    ) -> some View {
        if let categoryItem = categoryItem(for: categoryWrapper) {
            let categoryId = categoryIdentifier(for: categoryWrapper)
            let isExpanded = expandedCategoryIds.contains(categoryId)
            let isSelected = selectedDrawerItem == categoryItem

            Group {
                categoryHeader(
                    categoryItem: categoryItem,
                    categoryId: categoryId,
                    isExpanded: isExpanded,
                    isSelected: isSelected
                )

                if isExpanded {
                    feedSourcesList(drawerItems: drawerItems, trailingInset: Spacing.regular)
                }
            }
        } else {
            EmptyView()
        }
    }

    @ViewBuilder
    func makeUncategorizedDropdown(drawerItems: [DrawerItem]) -> some View {
        if drawerItems.isEmpty {
            EmptyView()
        } else {
            let categoryId = categoryIdentifier(for: nil)
            let isExpanded = expandedCategoryIds.contains(categoryId)
            let unreadCount = drawerItems
                .compactMap { $0 as? DrawerItem.DrawerFeedSource }
                .map(\.unreadCount)
                .reduce(0 as Int64, +)
            let uncategorizedCategory = DrawerItem.DrawerCategory(
                category: FeedSourceCategory(id: categoryId, title: feedFlowStrings.noCategory),
                unreadCount: 0
            )
            let isSelected = selectedDrawerItem == uncategorizedCategory

            Group {
                uncategorizedHeader(
                    categoryId: categoryId,
                    unreadCount: unreadCount,
                    isExpanded: isExpanded,
                    isSelected: isSelected,
                    uncategorizedCategory: uncategorizedCategory
                )

                if isExpanded {
                    feedSourcesList(drawerItems: drawerItems, trailingInset: Spacing.small)
                }
            }
        }
    }

    @ViewBuilder
    func categoryHeader(
        categoryItem: DrawerItem.DrawerCategory,
        categoryId: String,
        isExpanded: Bool,
        isSelected: Bool
    ) -> some View {
        HStack(spacing: Spacing.xsmall) {
            HStack {
                Text(categoryItem.category.title)
                Spacer()
                makeCategoryUnreadBadge(unreadCount: categoryItem.unreadCount)
            }
            .contentShape(Rectangle())
            .onTapGesture {
                selectCategory(categoryItem)
            }
            .foregroundStyle(isSelected ? Color.accentColor : Color.primary)
            .contextMenu {
                categoryContextMenu(categoryItem: categoryItem)
            }

            expansionToggleButton(isExpanded: isExpanded) {
                toggleCategoryExpansion(for: categoryId)
            }
        }
        .tag(categoryItem)
        .listRowBackground(
            selectionBackground(isSelected: isSelected)
        )
    }

    @ViewBuilder
    func uncategorizedHeader(
        categoryId: String,
        unreadCount: Int64,
        isExpanded: Bool,
        isSelected: Bool,
        uncategorizedCategory: DrawerItem.DrawerCategory
    ) -> some View {
        HStack(spacing: Spacing.xsmall) {
            HStack {
                Text(feedFlowStrings.noCategory)
                Spacer()
                makeCategoryUnreadBadge(unreadCount: unreadCount)
            }
            .contentShape(Rectangle())
            .onTapGesture {
                self.selectedDrawerItem = uncategorizedCategory
                self.onFeedFilterSelected(FeedFilter.Uncategorized())
            }
            .foregroundStyle(isSelected ? Color.accentColor : Color.primary)

            expansionToggleButton(isExpanded: isExpanded) {
                toggleCategoryExpansion(for: categoryId)
            }
        }
        .tag(uncategorizedCategory)
        .listRowBackground(
            selectionBackground(isSelected: isSelected)
        )
    }

    @ViewBuilder
    func feedSourcesList(drawerItems: [DrawerItem], trailingInset: CGFloat) -> some View {
        ForEach(drawerItems, id: \.self) { drawerItem in
            if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
                    .listRowInsets(
                        EdgeInsets(
                            top: Spacing.small,
                            leading: Spacing.medium,
                            bottom: Spacing.small,
                            trailing: trailingInset
                        )
                    )
            } else {
                EmptyView()
            }
        }
    }

    @ViewBuilder
    func expansionToggleButton(isExpanded: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.secondary)
                .padding(.leading, Spacing.medium)
                .padding(.trailing, Spacing.small)
                .padding(.vertical, Spacing.xxsmall)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    func categoryContextMenu(categoryItem: DrawerItem.DrawerCategory) -> some View {
        Button {
            editedCategoryName = categoryItem.category.title
            categoryToEdit = categoryItem.category.id
            showEditCategoryDialog = true
        } label: {
            Label(feedFlowStrings.editFeedSourceNameButton, systemImage: "pencil")
        }

        Button(role: .destructive) {
            categoryToDelete = categoryItem.category.id
            showDeleteCategoryDialog = true
        } label: {
            Label(feedFlowStrings.deleteFeed, systemImage: "trash")
        }
    }

    func selectCategory(_ categoryItem: DrawerItem.DrawerCategory) {
        self.selectedDrawerItem = categoryItem
        self.onFeedFilterSelected(FeedFilter.Category(feedCategory: categoryItem.category))
    }

    func toggleCategoryExpansion(for categoryId: String) {
        if expandedCategoryIds.contains(categoryId) {
            expandedCategoryIds.remove(categoryId)
        } else {
            expandedCategoryIds.insert(categoryId)
        }
    }

    func categoryIdentifier(
        for wrapper: DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper?
    ) -> String {
        wrapper?.feedSourceCategory?.id ?? "uncategorized"
    }

    func categoryItem(
        for wrapper: DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper
    ) -> DrawerItem.DrawerCategory? {
        guard let feedSourceCategory = wrapper.feedSourceCategory else {
            return nil
        }

        if let category = navDrawerState.categories
            .compactMap({ $0 as? DrawerItem.DrawerCategory })
            .first(where: { $0.category.id == feedSourceCategory.id }) {
            return category
        }

        return DrawerItem.DrawerCategory(category: feedSourceCategory, unreadCount: 0)
    }

    @ViewBuilder
    func makeCategoryUnreadBadge(unreadCount: Int64) -> some View {
        if unreadCount > 0 {
            Text("\(unreadCount)")
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.horizontal, 8)
                .background(Color.secondary.opacity(0.2))
                .clipShape(Capsule())
        }
    }

    @ViewBuilder
    func selectionOverlay(isSelected: Bool) -> some View {
        if isSelected {
            if appState.sizeClass == .compact {
                Color(.systemGray5)
            } else {
                RoundedRectangle(cornerRadius: 36, style: .continuous)
                    .fill(Color(.systemGray5))
                    .padding(.horizontal, Spacing.xsmall)
                    .padding(.vertical, Spacing.xxsmall)
            }
        } else {
            Color.clear
        }
    }

    @ViewBuilder
    func selectionBackground(isSelected: Bool) -> some View {
        if appState.sizeClass == .compact {
            Color(.systemBackground)
                .overlay(selectionOverlay(isSelected: isSelected))
        } else {
            selectionOverlay(isSelected: isSelected)
        }
    }
}
