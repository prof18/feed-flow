//
//  EditCategorySheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 20/01/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

// MARK: - Container for AddFeedViewModel
struct EditCategorySheetContainer: View {
    let viewModel: AddFeedViewModel
    let categorySelectorObserver: CategorySelectorObserver
    let onSave: () -> Void

    @State private var categoryState = CategoriesState(categories: [], isLoading: false)
    @State private var showAddCategoryDialog = false
    @State private var categoryToDelete: String?
    @State private var categoryToEdit: CategoriesState.CategoryItem?
    @State private var newCategoryName = ""

    var body: some View {
        EditCategorySheet(
            categoryItems: categoryState.categories,
            isLoading: categoryState.isLoading,
            showAddCategoryDialog: $showAddCategoryDialog,
            categoryToDelete: $categoryToDelete,
            categoryToEdit: $categoryToEdit,
            newCategoryName: $newCategoryName,
            onCategorySelected: { categoryId in
                categorySelectorObserver.onCategorySelected?(categoryId)
            },
            onAddCategory: { categoryName in
                viewModel.addNewCategory(categoryName: categoryName)
            },
            onDeleteCategory: { categoryId in
                viewModel.deleteCategory(categoryId: categoryId)
            },
            onEditCategory: { categoryId, newName in
                viewModel.editCategory(categoryId: CategoryId(value: categoryId), newName: newName)
            },
            onSave: onSave
        )
        .task {
            for await state in viewModel.categoriesState {
                await MainActor.run {
                    categoryState = state
                }
            }
        }
    }
}

// MARK: - Container for EditFeedViewModel
struct EditCategorySheetContainerForEdit: View {
    let viewModel: EditFeedViewModel
    let categorySelectorObserver: CategorySelectorObserver
    let onSave: () -> Void

    @State private var categoryState = CategoriesState(categories: [], isLoading: false)
    @State private var showAddCategoryDialog = false
    @State private var categoryToDelete: String?
    @State private var categoryToEdit: CategoriesState.CategoryItem?
    @State private var newCategoryName = ""

    var body: some View {
        EditCategorySheet(
            categoryItems: categoryState.categories,
            isLoading: categoryState.isLoading,
            showAddCategoryDialog: $showAddCategoryDialog,
            categoryToDelete: $categoryToDelete,
            categoryToEdit: $categoryToEdit,
            newCategoryName: $newCategoryName,
            onCategorySelected: { categoryId in
                categorySelectorObserver.onCategorySelected?(categoryId)
            },
            onAddCategory: { categoryName in
                viewModel.addNewCategory(categoryName: categoryName)
            },
            onDeleteCategory: { categoryId in
                viewModel.deleteCategory(categoryId: categoryId)
            },
            onEditCategory: { categoryId, newName in
                viewModel.editCategory(categoryId: CategoryId(value: categoryId), newName: newName)
            },
            onSave: onSave
        )
        .task {
            for await state in viewModel.categoriesState {
                await MainActor.run {
                    categoryState = state
                }
            }
        }
    }
}

// MARK: - Container for ChangeFeedCategoryViewModel
struct EditCategorySheetForChangeCategory: View {
    let viewModel: ChangeFeedCategoryViewModel
    let onSave: () -> Void

    @State private var categoryState = CategoriesState(categories: [], isLoading: false)
    @State private var showAddCategoryDialog = false
    @State private var categoryToDelete: String?
    @State private var categoryToEdit: CategoriesState.CategoryItem?
    @State private var newCategoryName = ""

    var body: some View {
        EditCategorySheet(
            categoryItems: categoryState.categories,
            isLoading: categoryState.isLoading,
            showAddCategoryDialog: $showAddCategoryDialog,
            categoryToDelete: $categoryToDelete,
            categoryToEdit: $categoryToEdit,
            newCategoryName: $newCategoryName,
            onCategorySelected: { categoryId in
                viewModel.onCategorySelected(categoryId: CategoryId(value: categoryId))
            },
            onAddCategory: { categoryName in
                viewModel.addNewCategory(categoryName: categoryName)
            },
            onDeleteCategory: { categoryId in
                viewModel.deleteCategory(categoryId: categoryId)
            },
            onEditCategory: { categoryId, newName in
                viewModel.editCategory(categoryId: CategoryId(value: categoryId), newName: newName)
            },
            onSave: onSave
        )
        .task {
            for await state in viewModel.categoriesState {
                await MainActor.run {
                    categoryState = state
                }
            }
        }
    }
}
