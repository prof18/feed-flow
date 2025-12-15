//
//  EditCategorySheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 20/01/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

// MARK: - Container that observes ViewModel directly
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

struct EditCategorySheet: View {
    @Environment(\.dismiss)
    private var dismiss

    let categoryItems: [CategoriesState.CategoryItem]
    let isLoading: Bool
    @Binding var showAddCategoryDialog: Bool
    @Binding var categoryToDelete: String?
    @Binding var categoryToEdit: CategoriesState.CategoryItem?
    @Binding var newCategoryName: String
    let onCategorySelected: (String) -> Void
    let onAddCategory: (CategoryName) -> Void
    let onDeleteCategory: (String) -> Void
    let onEditCategory: (String, CategoryName) -> Void
    let onSave: () -> Void

    private var backgroundColor: Color {
        Color(UIColor.systemBackground)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                backgroundColor.ignoresSafeArea()

                VStack(spacing: 0) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: Spacing.regular) {
                            Text(feedFlowStrings.addFeedCategoriesTitle)
                                .font(.title2)
                                .fontWeight(.bold)
                                .padding(.horizontal, Spacing.regular)
                                .padding(.top, Spacing.small)

                            FlowLayout(spacing: Spacing.small) {
                                ForEach(categoryItems, id: \.id) { category in
                                    CategoryChip(
                                        label: category.name ?? feedFlowStrings.noCategorySelectedHeader,
                                        isSelected: category.isSelected,
                                        onTap: {
                                            onCategorySelected(category.id)
                                        },
                                        onEditTap: category.name != nil ? {
                                            categoryToEdit = category
                                        } : nil,
                                        onDeleteTap: category.name != nil ? {
                                            categoryToDelete = category.id
                                        } : nil,
                                        usePrimaryColor: category.name != nil
                                    )
                                }
                            }
                            .padding(.horizontal, Spacing.regular)
                        }
                    }

                    VStack(spacing: Spacing.small) {
                        Button(
                            action: {
                                showAddCategoryDialog = true
                            },
                            label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "plus")
                                        .font(.system(size: 16, weight: .medium))
                                    Text(feedFlowStrings.addFeedCategoryTitle)
                                        .font(.body)
                                        .fontWeight(.medium)
                                }
                                .foregroundColor(.accentColor)
                                .padding(.vertical, 12)
                            }
                        )
                        .buttonStyle(.plain)

                        Button(
                            action: {
                                onSave()
                                dismiss()
                            },
                            label: {
                                Text(feedFlowStrings.actionSave)
                                    .fontWeight(.medium)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                            }
                        )
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(.horizontal, Spacing.regular)
                    .padding(.bottom, Spacing.medium)
                }

                if isLoading {
                    Color(UIColor.systemBackground)
                        .opacity(0.95)
                        .ignoresSafeArea()

                    ProgressView()
                        .scaleEffect(1.5)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        onSave()
                        dismiss()
                    } label: {
                        if isiOS26OrLater() {
                            Image(systemName: "xmark")
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "xmark.circle")
                        }
                    }
                }
            }
        }
        .presentationDetents([.fraction(0.6), .large])
        .presentationDragIndicator(.visible)
        .sheet(
            isPresented: $showAddCategoryDialog,
            onDismiss: {
                newCategoryName = ""
            }
        ) {
            AddCategoryNameSheet(
                categoryName: $newCategoryName,
                onConfirm: { categoryName in
                    onAddCategory(CategoryName(name: categoryName))
                }
            )
        }
        .confirmationDialog(
            feedFlowStrings.deleteCategoryConfirmationTitle,
            isPresented: Binding(
                get: { categoryToDelete != nil },
                set: { if !$0 { categoryToDelete = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button(feedFlowStrings.deleteFeed, role: .destructive) {
                if let id = categoryToDelete {
                    onDeleteCategory(id)
                }
                categoryToDelete = nil
            }
            Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) {
                categoryToDelete = nil
            }
        } message: {
            Text(feedFlowStrings.deleteCategoryConfirmationMessage)
        }
        .sheet(
            isPresented: Binding(
                get: { categoryToEdit != nil },
                set: { if !$0 { categoryToEdit = nil } }
            )
        ) {
            if let category = categoryToEdit {
                EditCategoryNameSheet(
                    categoryName: category.name ?? "",
                    onConfirm: { newName in
                        onEditCategory(category.id, CategoryName(name: newName))
                        categoryToEdit = nil
                    }
                )
            }
        }
    }
}

private struct AddCategoryNameSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    @Binding var categoryName: String
    let onConfirm: (String) -> Void

    @FocusState private var isTextFieldFocused: Bool

    private var trimmed: String {
        categoryName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: Spacing.regular) {
                TextField(feedFlowStrings.newCategoryHint, text: $categoryName)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.words)
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .focused($isTextFieldFocused)

                Button(feedFlowStrings.confirmButton) {
                    onConfirm(trimmed)
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
                .disabled(trimmed.isEmpty)
            }
            .padding()
            .navigationTitle(feedFlowStrings.addFeedCategoryTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) {
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.height(240)])
        .presentationDragIndicator(.visible)
        .onAppear {
            DispatchQueue.main.async {
                isTextFieldFocused = true
            }
        }
    }
}

private struct EditCategoryNameSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    @State var categoryName: String
    let onConfirm: (String) -> Void

    @FocusState private var isTextFieldFocused: Bool

    private var trimmed: String {
        categoryName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: Spacing.regular) {
                TextField(feedFlowStrings.categoryName, text: $categoryName)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.words)
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .focused($isTextFieldFocused)

                Button(feedFlowStrings.actionSave) {
                    onConfirm(trimmed)
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
                .disabled(trimmed.isEmpty)
            }
            .padding()
            .navigationTitle(feedFlowStrings.editCategory)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) {
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.height(240)])
        .presentationDragIndicator(.visible)
        .onAppear {
            DispatchQueue.main.async {
                isTextFieldFocused = true
            }
        }
    }
}

// MARK: - Flow Layout
struct FlowLayout: Layout {
    var spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = computeLayout(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = computeLayout(proposal: proposal, subviews: subviews)
        for (index, subview) in subviews.enumerated() {
            subview.place(
                at: CGPoint(x: bounds.minX + result.positions[index].x,
                            y: bounds.minY + result.positions[index].y),
                proposal: .unspecified
            )
        }
    }

    private func computeLayout(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        var positions: [CGPoint] = []
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var lineHeight: CGFloat = 0
        var maxWidth: CGFloat = 0

        let proposedWidth = proposal.replacingUnspecifiedDimensions().width

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if currentX + size.width > proposedWidth && currentX > 0 {
                currentX = 0
                currentY += lineHeight + spacing
                lineHeight = 0
            }

            positions.append(CGPoint(x: currentX, y: currentY))

            currentX += size.width + spacing
            lineHeight = max(lineHeight, size.height)
            maxWidth = max(maxWidth, currentX - spacing)
        }

        return (CGSize(width: maxWidth, height: currentY + lineHeight), positions)
    }
}

// MARK: - Category Chip Component
struct CategoryChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void
    let onEditTap: (() -> Void)?
    let onDeleteTap: (() -> Void)?
    var usePrimaryColor: Bool = true

    @Environment(\.colorScheme)
    private var colorScheme

    private var hasContextMenu: Bool {
        onEditTap != nil || onDeleteTap != nil
    }

    var body: some View {
        HStack(spacing: 6) {
            if isSelected {
                Image(systemName: "checkmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(usePrimaryColor ? .accentColor : .primary)
            }

            Text(label)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .medium)
                .foregroundColor(isSelected && usePrimaryColor ? .accentColor : .primary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(backgroundColor)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .strokeBorder(borderColor, lineWidth: isSelected ? 2 : 1)
        )
        .contentShape(Rectangle())
        .onTapGesture {
            onTap()
        }
        .contextMenu {
            if let onEditTap = onEditTap {
                Button(action: onEditTap) {
                    Label(feedFlowStrings.editCategory, systemImage: "pencil")
                }
            }
            if let onDeleteTap = onDeleteTap {
                Button(role: .destructive, action: onDeleteTap) {
                    Label(feedFlowStrings.deleteFeed, systemImage: "trash")
                }
            }
        }
    }

    private var backgroundColor: Color {
        if isSelected {
            if usePrimaryColor {
                return Color.accentColor.opacity(colorScheme == .dark ? 0.25 : 0.15)
            } else {
                return Color.gray.opacity(0.2)
            }
        } else {
            return Color.clear
        }
    }

    private var borderColor: Color {
        if isSelected {
            return usePrimaryColor ? Color.accentColor : Color.gray.opacity(0.5)
        } else {
            return Color(UIColor.separator)
        }
    }
}

// MARK: - Preview
#if DEBUG
@available(iOS 17.0, *)
#Preview {
    @Previewable @State var categoryItems: [CategoriesState.CategoryItem] = [
        CategoriesState.CategoryItem(
            id: String(Int64.max),
            name: nil,
            isSelected: false
        ),
        CategoriesState.CategoryItem(
            id: "1",
            name: "Tech",
            isSelected: true
        ),
        CategoriesState.CategoryItem(
            id: "2",
            name: "News",
            isSelected: false
        ),
        CategoriesState.CategoryItem(
            id: "3",
            name: "Sports",
            isSelected: false
        )
    ]
    @Previewable @State var showAddDialog = false
    @Previewable @State var categoryToDelete: String? = nil
    @Previewable @State var categoryToEdit: CategoriesState.CategoryItem? = nil
    @Previewable @State var newCategoryName = ""
    return EditCategorySheet(
        categoryItems: categoryItems,
        isLoading: false,
        showAddCategoryDialog: $showAddDialog,
        categoryToDelete: $categoryToDelete,
        categoryToEdit: $categoryToEdit,
        newCategoryName: $newCategoryName,
        onCategorySelected: { _ in },
        onAddCategory: { _ in },
        onDeleteCategory: { _ in },
        onEditCategory: { _, _ in },
        onSave: {}
    )
}
#endif

