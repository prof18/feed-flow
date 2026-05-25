//
//  DrawerAccessibilityIdentifiers.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 25/05/26.
//  Copyright © 2026 FeedFlow. All rights reserved.
//

enum DrawerAccessibilityIdentifiers {
    static let menuButton = "drawer_menu_button"
    static let settingsButton = "drawer_settings_button"
    static let timeline = "drawer_timeline"
    static let read = "drawer_read"
    static let bookmarks = "drawer_bookmarks"

    static func category(_ categoryId: String?) -> String {
        "drawer_category_\(categoryId?.e2eIdSuffix ?? "uncategorized")"
    }

    static func categoryExpand(_ categoryId: String?) -> String {
        "drawer_category_expand_\(categoryId?.e2eIdSuffix ?? "uncategorized")"
    }

    static func feedSource(_ feedSourceId: String) -> String {
        "drawer_feed_source_\(feedSourceId.e2eIdSuffix)"
    }
}

private extension String {
    var e2eIdSuffix: String {
        map { character in
            character.isLetter || character.isNumber || character == "_" ? character : "_"
        }
        .map(String.init)
        .joined()
    }
}
