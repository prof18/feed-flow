package com.prof18.feedflow.android.feedsourcelist

internal object FeedSourceListE2eIds {
    const val SETTINGS_ROW = "feed_source_list_settings_row"
    const val SCREEN = "feed_source_list_screen"
    const val EDIT_TOGGLE = "feed_source_list_edit_toggle"

    fun category(categoryId: String?): String =
        "feed_source_list_category_${categoryId.toE2eIdSuffix()}"

    fun categoryReorderHandle(categoryId: String?): String =
        "feed_source_list_category_reorder_${categoryId.toE2eIdSuffix()}"

    fun row(feedSourceId: String): String =
        "feed_source_list_row_${feedSourceId.toE2eIdSuffix()}"

    fun reorderHandle(feedSourceId: String): String =
        "feed_source_list_reorder_${feedSourceId.toE2eIdSuffix()}"

    fun warning(feedSourceId: String): String =
        "feed_source_list_warning_${feedSourceId.toE2eIdSuffix()}"

    fun renameInput(feedSourceId: String): String =
        "feed_source_list_rename_input_${feedSourceId.toE2eIdSuffix()}"

    fun renameSave(feedSourceId: String): String =
        "feed_source_list_rename_save_${feedSourceId.toE2eIdSuffix()}"
}

private fun String?.toE2eIdSuffix(): String =
    if (this == null) {
        "no_category"
    } else {
        map { char ->
            if (char.isLetterOrDigit() || char == '_') {
                char
            } else {
                '_'
            }
        }.joinToString(separator = "")
    }
