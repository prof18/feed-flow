//
//  CategoriesSection.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct CategoriesSection: View {
    let categories: [DrawerItem]
    let onSelect: (DrawerItem) -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void
    let onDeleteCategory: (String) -> Void
    let onUpdateCategoryName: (String, String) -> Void

    @State private var showDeleteCategoryDialog = false
    @State private var showEditCategoryDialog = false
    @State private var categoryToDelete: String?
    @State private var categoryToEdit: String?
    @State private var editedCategoryName: String = ""

    var body: some View {
        if !categories.isEmpty {
            Section(
                content: {
                    ForEach(categories, id: \.self) { drawerItem in
                        if let categoryItem = drawerItem as? DrawerItem.DrawerCategory {
                            HStack {
                                Label(categoryItem.category.title, systemImage: "tag")
                                Spacer()
                                if categoryItem.unreadCount > 0 {
                                    Text("\(categoryItem.unreadCount)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                        .padding(.horizontal, 8)
                                        .background(Color.secondary.opacity(0.2))
                                        .clipShape(Capsule())
                                }
                            }
                            .contentShape(Rectangle())
                            .onTapGesture {
                                onSelect(categoryItem)
                                onFeedFilterSelected(
                                    FeedFilter.Category(feedCategory: categoryItem.category))
                            }
                            .contextMenu {
                                Button {
                                    editedCategoryName = categoryItem.category.title
                                    categoryToEdit = categoryItem.category.id
                                    showEditCategoryDialog = true
                                } label: {
                                    Label(feedFlowStrings.editCategory, systemImage: "pencil")
                                }

                                Button(role: .destructive) {
                                    categoryToDelete = categoryItem.category.id
                                    showDeleteCategoryDialog = true
                                } label: {
                                    Label(feedFlowStrings.deleteFeed, systemImage: "trash")
                                }
                            }
                        }
                    }
                },
                header: {
                    Text(feedFlowStrings.drawerTitleCategories)
                }
            )
            .overlay(
                DeleteCategoryDialog(
                    isPresented: $showDeleteCategoryDialog,
                    categoryToDelete: $categoryToDelete,
                    onDelete: onDeleteCategory
                )
            )
            .overlay(
                EditCategoryDialog(
                    isPresented: $showEditCategoryDialog,
                    categoryToEdit: $categoryToEdit,
                    editedCategoryName: $editedCategoryName,
                    onSave: onUpdateCategoryName
                )
            )
        }
    }
}
