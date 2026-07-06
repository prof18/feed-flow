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
    var appState

    @Environment(BrowserSelector.self)
    var browserSelector: BrowserSelector

    @Environment(\.openURL)
    var openURL

    @Binding var selectedSidebarItem: SidebarSelection?

    @StateObject var categoryVMStoreOwner = VMStoreOwner<ChangeFeedCategoryViewModel>(
        Deps.shared.getChangeFeedCategoryViewModel()
    )

    @State private var showMarkAllReadDialog = false
    @State private var showClearOldArticlesDialog = false
    @State var expandedCategoryIds: Set<String> = []
    @State var showDeleteCategoryDialog = false
    @State var showEditCategoryDialog = false
    @State var categoryToDelete: String?
    @State var categoryToEdit: String?
    @State var editedCategoryName: String = ""
    @State var showChangeCategorySheet = false
    @State var selectedFeedForCategoryChange: FeedSource?
    @State private var showFeedSuggestionsSheet = false
    @State var showDeleteFeedDialog = false
    @State var feedToDelete: FeedSource?
    @State var showDeleteAllFeedsDialog = false
    @State var categoryToDeleteAllFeeds: String?

    let navDrawerState: NavDrawerState
    let onFeedFilterSelected: (FeedFilter) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let deleteAllFeeds: () -> Void
    let onShowSettingsClick: () -> Void
    let onAddFeedClick: () -> Void
    let onImportExportClick: () -> Void
    let onEditFeedClick: (FeedSource) -> Void
    let onDeleteFeedClick: (FeedSource) -> Void
    let onPinFeedClick: (FeedSource) -> Void
    let onMarkAllReadForFeedSource: (FeedSource) -> Void
    let onMarkAllReadForCategory: (FeedSourceCategory) -> Void
    let onDeleteAllFeedsInCategory: (String) -> Void
    let onDeleteCategory: (String) -> Void
    let onUpdateCategoryName: (String, String) -> Void
    let validateCategoryName: (String, CategoryName) -> CategoryNameValidationResult
    let onReorderPinnedFeedSources: ([FeedSource]) -> Void
    let onReorderCategories: ([DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper]) -> Void
    let onReorderFeedSources: ([FeedSource]) -> Void

    var isCompactPhone: Bool {
        UIDevice.current.userInterfaceIdiom == .phone
    }

    @ViewBuilder private var styledList: some View {
        if isCompactPhone {
            sidebarList
                .listStyle(.sidebar)
                .scrollContentBackground(.hidden)
                .background(Color(.systemGroupedBackground))
        } else {
            sidebarList.listStyle(.sidebar)
        }
    }

    private var sidebarList: some View {
        List {
            Section(feedFlowStrings.drawerTitleLibrary) {
                TimelineSection(
                    timeline: navDrawerState.timeline,
                    isSelected: selectedSidebarItem == .timeline,
                    isCompact: isCompactPhone,
                    onSelect: { self.selectedSidebarItem = .timeline },
                    onFeedFilterSelected: self.onFeedFilterSelected
                )

                ReadSection(
                    read: navDrawerState.read,
                    isSelected: selectedSidebarItem == .read,
                    isCompact: isCompactPhone,
                    onSelect: { self.selectedSidebarItem = .read },
                    onFeedFilterSelected: self.onFeedFilterSelected
                )

                BookmarksSection(
                    bookmarks: navDrawerState.bookmarks,
                    isSelected: selectedSidebarItem == .bookmarks,
                    isCompact: isCompactPhone,
                    onSelect: { self.selectedSidebarItem = .bookmarks },
                    onFeedFilterSelected: self.onFeedFilterSelected
                )
            }

            if !navDrawerState.pinnedFeedSources.isEmpty {
                Section(feedFlowStrings.drawerTitlePinnedFeeds) {
                    pinnedFeedSourcesContent
                }
            }

            if !navDrawerState.feedSourcesByCategory.isEmpty ||
                !navDrawerState.feedSourcesWithoutCategory.isEmpty ||
                !navDrawerState.categories.isEmpty {
                Section(feedFlowStrings.feedsTitle) {
                    feedSourcesContent
                }
            }
        }
    }

    var body: some View {
        styledList
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(
                        action: onShowSettingsClick,
                        label: { Image(systemName: "gear") }
                    )
                    .accessibilityIdentifier(DrawerAccessibilityIdentifiers.settingsButton)
                }

                ToolbarItem(placement: .primaryAction) {
                    Menu(
                        content: {
                            Button(
                                action: onAddFeedClick,
                                label: { Label(feedFlowStrings.addFeed, systemImage: "plus.circle") }
                            )
                            Button(
                                action: { showFeedSuggestionsSheet = true },
                                label: { Label(feedFlowStrings.feedSuggestionsTitle, systemImage: "lightbulb") }
                            )
                            .accessibilityIdentifier(FeedSuggestionsIds.drawerItem)
                            Button(
                                action: onImportExportClick,
                                label: { Label(feedFlowStrings.importFeedButton, systemImage: "square.and.arrow.down") }
                            )
                        },
                        label: { Image(systemName: "plus") }
                    )
                    .accessibilityIdentifier(FeedSuggestionsIds.menuButton)
                }
            }
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
            DeleteFeedSourceDialog(
                isPresented: $showDeleteFeedDialog,
                feedToDelete: $feedToDelete,
                onDelete: onDeleteFeedClick
            )
        )
        .background(
            DeleteAllFeedsInCategoryDialog(
                isPresented: $showDeleteAllFeedsDialog,
                categoryToDeleteAllFeeds: $categoryToDeleteAllFeeds,
                onDeleteAllFeeds: onDeleteAllFeedsInCategory
            )
        )
        .background(
            EditCategoryDialog(
                isPresented: $showEditCategoryDialog,
                categoryToEdit: $categoryToEdit,
                editedCategoryName: $editedCategoryName,
                validateCategoryName: validateCategoryName,
                onSave: onUpdateCategoryName
            )
        )
        .sheet(isPresented: $showFeedSuggestionsSheet) {
            FeedSuggestionsScreen()
        }
        .sheet(isPresented: $showChangeCategorySheet) {
            EditCategorySheetForChangeCategory(
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
