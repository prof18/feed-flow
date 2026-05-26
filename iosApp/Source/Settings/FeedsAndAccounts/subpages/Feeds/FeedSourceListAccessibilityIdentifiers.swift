import Foundation

enum FeedSourceListAccessibilityIdentifiers {
    static let settingsRow = "feed_source_list_settings_row"
    static let screen = "feed_source_list_screen"

    static func category(_ categoryId: String?) -> String {
        "feed_source_list_category_\(categoryId.e2eIdSuffix)"
    }

    static func row(_ feedSourceId: String) -> String {
        "feed_source_list_row_\(feedSourceId.e2eIdSuffix)"
    }

    static func warning(_ feedSourceId: String) -> String {
        "feed_source_list_warning_\(feedSourceId.e2eIdSuffix)"
    }

    static func renameInput(_ feedSourceId: String) -> String {
        "feed_source_list_rename_input_\(feedSourceId.e2eIdSuffix)"
    }

    static func renameSave(_ feedSourceId: String) -> String {
        "feed_source_list_rename_save_\(feedSourceId.e2eIdSuffix)"
    }
}

private extension Optional where Wrapped == String {
    var e2eIdSuffix: String {
        switch self {
        case .some(let value):
            value.e2eIdSuffix
        case .none:
            "no_category"
        }
    }
}

private extension String {
    var e2eIdSuffix: String {
        map { char in
            char.isLetter || char.isNumber || char == "_" ? String(char) : "_"
        }.joined()
    }
}
