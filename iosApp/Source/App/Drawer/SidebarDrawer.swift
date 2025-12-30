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

    @Binding var selectedDrawerItem: DrawerItem?

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

            FeedSuggestionsSection(onFeedSuggestionsClick: onFeedSuggestionsClick)

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
