import FeedFlowKit
import NukeUI
import SwiftUI

extension SidebarDrawer {
    @ViewBuilder var pinnedFeedSourcesContent: some View {
        let pinnedFeedSources = navDrawerState.pinnedFeedSources
            .compactMap { $0 as? DrawerItem.DrawerFeedSource }

        ForEach(pinnedFeedSources, id: \.feedSource.id) { drawerFeedSource in
            makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
        }
        .onMove(perform: pinnedFeedSourcesMoveAction(pinnedFeedSources))
    }

    @ViewBuilder var feedSourcesContent: some View {
        if !navDrawerState.feedSourcesByCategory.isEmpty || !navDrawerState.feedSourcesWithoutCategory.isEmpty {
            if navDrawerState.feedSourcesByCategory.isEmpty {
                makeUncategorizedDropdown(drawerItems: Array(navDrawerState.feedSourcesWithoutCategory))
            } else {
                let categoryWrappers = sortedCategoryWrappers

                ForEach(categoryWrappers, id: \.self) { category in
                    let categoryWrapper =
                        category as DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper

                    let drawerItems = navDrawerState.feedSourcesByCategory[categoryWrapper] ?? []
                    if categoryWrapper.feedSourceCategory == nil {
                        makeUncategorizedDropdown(drawerItems: drawerItems)
                    } else {
                        makeCategoryDropdown(
                            drawerItems: drawerItems,
                            categoryWrapper: categoryWrapper
                        )
                    }
                }
                .onMove(perform: categoryWrappersMoveAction(categoryWrappers))
            }
        }
    }

    private var sortedCategoryWrappers: [DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper] {
        navDrawerState.feedSourcesByCategory.keys
            .map { $0 as DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper }
            .sorted { lhs, rhs in
                let lhsPosition = lhs.feedSourceCategory?.position ?? navDrawerState.uncategorizedPosition
                let rhsPosition = rhs.feedSourceCategory?.position ?? navDrawerState.uncategorizedPosition
                if lhsPosition == rhsPosition {
                    // The Uncategorized group (nil title) sorts first, matching the shared ordering
                    guard let lhsTitle = lhs.feedSourceCategory?.title else { return true }
                    guard let rhsTitle = rhs.feedSourceCategory?.title else { return false }
                    return lhsTitle < rhsTitle
                }
                return lhsPosition < rhsPosition
            }
    }

    @ViewBuilder
    func makeFeedSourceDrawerItem(drawerItem: DrawerItem.DrawerFeedSource) -> some View {
        let isSelected = selectedSidebarItem == .feedSource(id: drawerItem.feedSource.id)
        FeedSourceDrawerItem(
            drawerItem: drawerItem,
            onSelect: { item in
                self.selectedSidebarItem = .feedSource(id: item.feedSource.id)
                self.onFeedFilterSelected(FeedFilter.Source(feedSource: item.feedSource))
            },
            onEdit: onEditFeedClick,
            onPin: onPinFeedClick,
            onChangeCategory: { feedSource in
                selectedFeedForCategoryChange = feedSource
                categoryVMStoreOwner.instance.loadFeedSource(feedSource: feedSource)
                showChangeCategorySheet = true
            },
            onDelete: { feedSource in
                feedToDelete = feedSource
                showDeleteFeedDialog = true
            },
            onOpenWebsite: { url in
                if browserSelector.openInAppBrowser() {
                    guard let url = URL(string: url) else { return }
                    self.appState.openInAppBrowser(url: url)
                } else {
                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url))
                }
            },
            onMarkAllRead: onMarkAllReadForFeedSource
        )
        .tag(SidebarSelection.feedSource(id: drawerItem.feedSource.id))
        .listRowBackground(sidebarSelectionBackground(isSelected: isSelected, isCompact: isCompact))
    }

    var isCompact: Bool {
        isCompactPhone
    }

    @ViewBuilder
    func makeCategoryDropdown(
        drawerItems: [DrawerItem],
        categoryWrapper: DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper
    ) -> some View {
        if let categoryItem = categoryItem(for: categoryWrapper) {
            let categoryId = categoryIdentifier(for: categoryWrapper)
            let isExpanded = expandedCategoryIds.contains(categoryId)
            let isSelected = selectedSidebarItem == .category(id: categoryItem.category.id)

            Group {
                categoryHeader(
                    categoryItem: categoryItem,
                    categoryId: categoryId,
                    isExpanded: isExpanded,
                    isSelected: isSelected
                )

                if isExpanded {
                    feedSourcesList(drawerItems: drawerItems)
                }
            }
        } else {
            EmptyView()
        }
    }

    @ViewBuilder
    func makeUncategorizedDropdown(drawerItems: [DrawerItem]) -> some View {
        if !drawerItems.isEmpty {
            let categoryId = categoryIdentifier(for: nil)
            let isExpanded = expandedCategoryIds.contains(categoryId)
            let unreadCount = drawerItems
                .compactMap { $0 as? DrawerItem.DrawerFeedSource }
                .map(\.unreadCount)
                .reduce(0 as Int64, +)
            let isSelected = selectedSidebarItem == .category(id: categoryId)

            Group {
                HStack(spacing: Spacing.xsmall) {
                    Button {
                        self.selectedSidebarItem = .category(id: categoryId)
                        self.onFeedFilterSelected(FeedFilter.Uncategorized())
                    } label: {
                        HStack {
                            Image(systemName: "folder")
                                .foregroundStyle(.secondary)
                            Text(feedFlowStrings.noCategory)
                            Spacer()
                            if unreadCount > 0 {
                                Text("\(unreadCount)")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.secondary.opacity(0.15))
                                    .clipShape(Capsule())
                            }
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Color.primary)
                    .accessibilityIdentifier(DrawerAccessibilityIdentifiers.category(nil))

                    expansionToggle(isExpanded: isExpanded) {
                        toggleCategoryExpansion(for: categoryId)
                    }
                    .accessibilityIdentifier(DrawerAccessibilityIdentifiers.categoryExpand(nil))
                }
                .tag(SidebarSelection.category(id: categoryId))
                .listRowBackground(categorySelectionBackground(isSelected: isSelected, isCompact: isCompact))

                if isExpanded {
                    feedSourcesList(drawerItems: drawerItems)
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
            Button {
                self.selectedSidebarItem = .category(id: categoryItem.category.id)
                self.onFeedFilterSelected(FeedFilter.Category(feedCategory: categoryItem.category))
            } label: {
                HStack {
                    Image(systemName: "folder")
                        .foregroundStyle(.secondary)
                    Text(categoryItem.category.title)
                    Spacer()
                    if categoryItem.unreadCount > 0 {
                        Text("\(categoryItem.unreadCount)")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.secondary.opacity(0.15))
                            .clipShape(Capsule())
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .foregroundStyle(Color.primary)
            .contextMenu {
                categoryContextMenu(categoryItem: categoryItem)
            }
            .accessibilityIdentifier(DrawerAccessibilityIdentifiers.category(categoryId))

            expansionToggle(isExpanded: isExpanded) {
                toggleCategoryExpansion(for: categoryId)
            }
            .accessibilityIdentifier(DrawerAccessibilityIdentifiers.categoryExpand(categoryId))
        }
        .tag(SidebarSelection.category(id: categoryItem.category.id))
        .listRowBackground(categorySelectionBackground(isSelected: isSelected, isCompact: isCompact))
    }

    @ViewBuilder
    func feedSourcesList(drawerItems: [DrawerItem]) -> some View {
        let drawerFeedSources = drawerItems.compactMap { $0 as? DrawerItem.DrawerFeedSource }

        ForEach(drawerFeedSources, id: \.feedSource.id) { drawerFeedSource in
            makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
                .listRowInsets(
                    EdgeInsets(
                        top: Spacing.small,
                        leading: Spacing.medium,
                        bottom: Spacing.small,
                        trailing: Spacing.regular
                    )
                )
        }
        .onMove(perform: drawerFeedSourcesMoveAction(drawerFeedSources))
    }

    func pinnedFeedSourcesMoveAction(
        _ pinnedFeedSources: [DrawerItem.DrawerFeedSource]
    ) -> ((IndexSet, Int) -> Void)? {
        guard pinnedFeedSources.count > 1 else { return nil }
        return { source, destination in
            var reorderedFeedSources = pinnedFeedSources
            reorderedFeedSources.move(fromOffsets: source, toOffset: destination)
            onReorderPinnedFeedSources(reorderedFeedSources.map(\.feedSource))
        }
    }

    func categoryWrappersMoveAction(
        _ categoryWrappers: [DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper]
    ) -> ((IndexSet, Int) -> Void)? {
        guard categoryWrappers.count > 1 else { return nil }
        return { source, destination in
            var reorderedCategories = categoryWrappers
            reorderedCategories.move(fromOffsets: source, toOffset: destination)
            onReorderCategories(reorderedCategories)
        }
    }

    func drawerFeedSourcesMoveAction(
        _ drawerFeedSources: [DrawerItem.DrawerFeedSource]
    ) -> ((IndexSet, Int) -> Void)? {
        guard drawerFeedSources.count > 1 else { return nil }
        return { source, destination in
            var reorderedFeedSources = drawerFeedSources
            reorderedFeedSources.move(fromOffsets: source, toOffset: destination)
            onReorderFeedSources(reorderedFeedSources.map(\.feedSource))
        }
    }

    @ViewBuilder
    func expansionToggle(isExpanded: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.secondary)
                .padding(.leading, Spacing.small)
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
            Label(feedFlowStrings.renameCategory, systemImage: "pencil")
        }
        .accessibilityIdentifier(DrawerAccessibilityIdentifiers.categoryMenuRename)

        if categoryItem.unreadCount > 0 {
            Button {
                onMarkAllReadForCategory(categoryItem.category)
            } label: {
                Label(feedFlowStrings.markAllReadButton, systemImage: "checkmark.circle")
            }
        }

        Divider()

        Button(role: .destructive) {
            categoryToDeleteAllFeeds = categoryItem.category.id
            showDeleteAllFeedsDialog = true
        } label: {
            Label(feedFlowStrings.deleteAllFeedsInCategory, systemImage: "trash")
        }
        .accessibilityIdentifier(DrawerAccessibilityIdentifiers.categoryMenuDeleteAllFeeds)

        Button(role: .destructive) {
            categoryToDelete = categoryItem.category.id
            showDeleteCategoryDialog = true
        } label: {
            Label(feedFlowStrings.deleteCategory, systemImage: "trash")
        }
        .accessibilityIdentifier(DrawerAccessibilityIdentifiers.categoryMenuDeleteCategory)
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
    func categorySelectionBackground(isSelected: Bool, isCompact: Bool) -> some View {
        if isSelected && isCompact {
            Color(.systemGray4)
        } else if isSelected {
            RoundedRectangle(cornerRadius: 28)
                .fill(Color(.systemGray4))
        } else if isCompact {
            Color(.secondarySystemGroupedBackground)
        } else {
            Color.clear
        }
    }
}
