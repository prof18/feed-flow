package com.prof18.feedflow.android.home.drawer

internal object DrawerE2eIds {
    const val MENU_BUTTON = "drawer_menu_button"
    const val SETTINGS_BUTTON = "drawer_settings_button"
    const val TIMELINE = "drawer_timeline"
    const val READ = "drawer_read"
    const val BOOKMARKS = "drawer_bookmarks"

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
