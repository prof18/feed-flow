import FeedFlowKit
import NukeUI
import SwiftUI

extension SidebarDrawer {
    @ViewBuilder var pinnedFeedSourcesContent: some View {
        ForEach(navDrawerState.pinnedFeedSources, id: \.stableId) { drawerItem in
            if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
            }
        }
    }

    @ViewBuilder var feedSourcesContent: some View {
        if !navDrawerState.feedSourcesByCategory.isEmpty || !navDrawerState.feedSourcesWithoutCategory.isEmpty {
            let uncategorizedFromMap = navDrawerState.feedSourcesByCategory
                .filter { $0.key.feedSourceCategory == nil }
                .flatMap { $0.value }
            let uncategorizedItems: [DrawerItem] =
                Array(navDrawerState.feedSourcesWithoutCategory) + uncategorizedFromMap

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
                }
            }
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
            onDelete: onDeleteFeedClick,
            onOpenWebsite: { url in
                if browserSelector.openInAppBrowser() {
                    guard let url = URL(string: url) else { return }
                    self.appState.openInAppBrowser(url: url)
                } else {
                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url))
                }
            }
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
                    .onTapGesture {
                        self.selectedSidebarItem = .category(id: categoryId)
                        self.onFeedFilterSelected(FeedFilter.Uncategorized())
                    }
                    .foregroundStyle(Color.primary)

                    expansionToggle(isExpanded: isExpanded) {
                        toggleCategoryExpansion(for: categoryId)
                    }
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
            .onTapGesture {
                self.selectedSidebarItem = .category(id: categoryItem.category.id)
                self.onFeedFilterSelected(FeedFilter.Category(feedCategory: categoryItem.category))
            }
            .foregroundStyle(Color.primary)
            .contextMenu {
                categoryContextMenu(categoryItem: categoryItem)
            }

            expansionToggle(isExpanded: isExpanded) {
                toggleCategoryExpansion(for: categoryId)
            }
        }
        .tag(SidebarSelection.category(id: categoryItem.category.id))
        .listRowBackground(categorySelectionBackground(isSelected: isSelected, isCompact: isCompact))
    }

    @ViewBuilder
    func feedSourcesList(drawerItems: [DrawerItem]) -> some View {
        ForEach(drawerItems, id: \.stableId) { drawerItem in
            if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
                    .listRowInsets(
                        EdgeInsets(
                            top: Spacing.small,
                            leading: Spacing.medium,
                            bottom: Spacing.small,
                            trailing: Spacing.regular
                        )
                    )
            } else {
                EmptyView()
            }
        }
    }

    @ViewBuilder
    func expansionToggle(isExpanded: Bool, action: @escaping () -> Void) -> some View {
        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.secondary)
            .padding(.leading, Spacing.small)
            .padding(.trailing, Spacing.small)
            .padding(.vertical, Spacing.xxsmall)
            .contentShape(Rectangle())
            .onTapGesture(perform: action)
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
