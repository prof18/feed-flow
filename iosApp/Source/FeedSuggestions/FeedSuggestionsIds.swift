enum FeedSuggestionsIds {
    static let drawerItem = "feed_suggestions_drawer_item"
    static let menuButton = "feed_suggestions_menu_button"
    static let screen = "feed_suggestions_screen"

    static func category(_ categoryId: String) -> String {
        "feed_suggestions_category_\(categoryId.e2eIdSuffix)"
    }

    static func row(_ feedUrl: String) -> String {
        "feed_suggestions_row_\(feedUrl.e2eIdSuffix)"
    }

    static func addButton(_ feedUrl: String) -> String {
        "feed_suggestions_add_\(feedUrl.e2eIdSuffix)"
    }
}

private extension String {
    var e2eIdSuffix: String {
        map { char in
            char.isLetter || char.isNumber || char == "_" ? String(char) : "_"
        }.joined()
    }
}
