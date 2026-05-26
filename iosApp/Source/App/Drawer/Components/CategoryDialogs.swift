import FeedFlowKit
import SwiftUI

struct DeleteCategoryDialog: View {
    @Binding var isPresented: Bool
    @Binding var categoryToDelete: String?
    let onDelete: (String) -> Void

    var body: some View {
        EmptyView()
            .alert(feedFlowStrings.deleteCategoryConfirmationTitle, isPresented: $isPresented) {
                Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) {
                    categoryToDelete = nil
                }
                Button(feedFlowStrings.deleteCategory, role: .destructive) {
                    if let id = categoryToDelete {
                        onDelete(id)
                    }
                    categoryToDelete = nil
                }
            } message: {
                Text(feedFlowStrings.deleteCategoryConfirmationMessage)
            }
    }
}

struct EditCategoryDialog: View {
    @Binding var isPresented: Bool
    @Binding var categoryToEdit: String?
    @Binding var editedCategoryName: String
    let validateCategoryName: (String, CategoryName) -> CategoryNameValidationResult
    let onSave: (String, String) -> Void

    var body: some View {
        EmptyView()
            .sheet(
                isPresented: $isPresented,
                onDismiss: {
                    categoryToEdit = nil
                    editedCategoryName = ""
                },
                content: {
                    if let categoryId = categoryToEdit {
                        EditCategoryNameSheet(
                            categoryId: categoryId,
                            categoryName: $editedCategoryName,
                            validateCategoryName: validateCategoryName,
                            onSave: { newName in
                                onSave(categoryId, newName)
                                isPresented = false
                            }
                        )
                    }
                }
            )
    }
}

private struct EditCategoryNameSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    let categoryId: String
    @Binding var categoryName: String
    let validateCategoryName: (String, CategoryName) -> CategoryNameValidationResult
    let onSave: (String) -> Void

    @FocusState private var isTextFieldFocused: Bool

    private var validationResult: CategoryNameValidationResult {
        validateCategoryName(categoryId, CategoryName(name: categoryName))
    }

    private var hasDuplicateName: Bool {
        validationResult == .duplicate
    }

    private var canSave: Bool {
        validationResult == .valid
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
                    .accessibilityIdentifier(CategoryDialogAccessibilityIdentifiers.renameCategoryInput)

                #if DEBUG
                    Button {
                        categoryName = "E2E Renamed Category"
                    } label: {
                        Image(systemName: "textformat")
                    }
                    .buttonStyle(.bordered)
                    .accessibilityIdentifier(CategoryDialogAccessibilityIdentifiers.applyRenameCategoryNameButton)
                #endif

                if hasDuplicateName {
                    Text(feedFlowStrings.categoryNameAlreadyExists)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Button(feedFlowStrings.actionSave) {
                    onSave(categoryName)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!canSave)
                .accessibilityIdentifier(CategoryDialogAccessibilityIdentifiers.renameCategorySaveButton)
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
        .presentationDetents([.height(280)])
        .presentationDragIndicator(.visible)
        .onAppear {
            #if !DEBUG
            DispatchQueue.main.async {
                isTextFieldFocused = true
            }
            #endif
        }
    }
}

private enum CategoryDialogAccessibilityIdentifiers {
    static let renameCategoryInput = "edit_feed_category_rename_input"
    static let renameCategorySaveButton = "edit_feed_category_rename_save"
    static let applyRenameCategoryNameButton = "edit_feed_category_apply_e2e_rename"
}
