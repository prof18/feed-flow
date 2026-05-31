enum EditCategoryAccessibilityIdentifiers {
    static let saveButton = "edit_feed_category_sheet_save"
    static let addCategoryButton = "edit_feed_category_sheet_add"
    static let addCategoryInput = "edit_feed_category_add_input"
    static let addCategoryConfirmButton = "edit_feed_category_add_confirm"
    static let renameCategoryInput = "edit_feed_category_rename_input"
    static let renameCategorySaveButton = "edit_feed_category_rename_save"

    static func categoryChip(_ label: String) -> String {
        "edit_feed_category_\(label.e2eIdSuffix)"
    }
}

private extension String {
    var e2eIdSuffix: String {
        lowercased()
            .map { character in
                character.isLetter || character.isNumber || character == "_" ? character : "_"
            }
            .map(String.init)
            .joined()
    }
}
