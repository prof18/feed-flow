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
    @State var showFeedSuggestionsSheet = false

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

                FeedSuggestionsSection(
                    isCompact: isCompactPhone,
                    onFeedSuggestionsClick: { showFeedSuggestionsSheet = true }
                )
            }

            if !navDrawerState.pinnedFeedSources.isEmpty {
                Section(feedFlowStrings.drawerTitlePinnedFeeds) {
                    pinnedFeedSourcesContent
                }
            }

            Section(feedFlowStrings.feedsTitle) {
                feedSourcesContent
            }
        }
    }

    var body: some View {
        styledList
            .toolbar {
            ToolbarItemGroup(placement: .bottomBar) {
                Spacer()
                Button(action: onAddFeedClick) {
                    Image(systemName: "plus")
                }
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
            EditCategoryDialog(
                isPresented: $showEditCategoryDialog,
                categoryToEdit: $categoryToEdit,
                editedCategoryName: $editedCategoryName,
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
