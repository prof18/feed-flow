package com.prof18.feedflow.android.home.drawer

internal object DrawerE2eIds {
    const val MENU_BUTTON = "drawer_menu_button"
    const val SETTINGS_BUTTON = "drawer_settings_button"
    const val TIMELINE = "drawer_timeline"
    const val READ = "drawer_read"
    const val BOOKMARKS = "drawer_bookmarks"
    const val PINNED_REORDER_TOGGLE = "drawer_pinned_reorder_toggle"
    const val FEED_SOURCES_REORDER_TOGGLE = "drawer_feed_sources_reorder_toggle"
    const val CATEGORY_MENU_RENAME = "drawer_category_menu_rename"
    const val CATEGORY_MENU_DELETE_ALL_FEEDS = "drawer_category_menu_delete_all_feeds"
    const val CATEGORY_MENU_DELETE_CATEGORY = "drawer_category_menu_delete_category"

    fun category(categoryId: String?): String =
        "drawer_category_${categoryId?.toE2eIdSuffix() ?: "uncategorized"}"

    fun categoryExpand(categoryId: String?): String =
        "drawer_category_expand_${categoryId?.toE2eIdSuffix() ?: "uncategorized"}"

    fun feedSource(feedSourceId: String): String =
        "drawer_feed_source_${feedSourceId.toE2eIdSuffix()}"
}

private fun String.toE2eIdSuffix(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') {
            char
        } else {
            '_'
        }
    }.joinToString(separator = "")
