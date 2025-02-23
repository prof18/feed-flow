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
                Button(feedFlowStrings.deleteFeed, role: .destructive) {
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
    let onSave: (String, String) -> Void

    var body: some View {
        EmptyView()
            .alert(feedFlowStrings.editCategory, isPresented: $isPresented) {
                TextField(feedFlowStrings.categoryName, text: $editedCategoryName)

                Button(feedFlowStrings.actionSave, role: .none) {
                    if !editedCategoryName.isEmpty, let id = categoryToEdit {
                        onSave(id, editedCategoryName)
                    }
                    categoryToEdit = nil
                    editedCategoryName = ""
                }
                .disabled(editedCategoryName.isEmpty)

                Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) {
                    categoryToEdit = nil
                    editedCategoryName = ""
                }
            }
    }
}
