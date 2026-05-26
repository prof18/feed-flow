package com.prof18.feedflow.shared.ui.feed

object FeedFormE2eIds {
    const val ADD_FEED_URL_INPUT = "add_feed_url_input"
    const val ADD_FEED_SAVE_BUTTON = "add_feed_save_button"
    const val ADD_FEED_PREPARE_FORCE_ADD_FAILURE = "add_feed_prepare_force_add_failure"
    const val ADD_FEED_FORCE_ADD_ACKNOWLEDGE = "add_feed_force_add_acknowledge"
    const val ADD_FEED_FORCE_ADD_BUTTON = "add_feed_force_add_button"
    const val NAME_INPUT = "edit_feed_name"
    const val HIDDEN_TOGGLE = "edit_feed_hidden_toggle"
    const val PINNED_TOGGLE = "edit_feed_pinned_toggle"
    const val CATEGORY_SELECTOR = "edit_feed_category_selector"
    const val CATEGORY_SHEET_SAVE = "edit_feed_category_sheet_save"
    const val CATEGORY_SHEET_ADD = "edit_feed_category_sheet_add"
    const val CATEGORY_ADD_INPUT = "edit_feed_category_add_input"
    const val CATEGORY_ADD_CONFIRM = "edit_feed_category_add_confirm"
    const val CATEGORY_RENAME_INPUT = "edit_feed_category_rename_input"
    const val CATEGORY_RENAME_SAVE = "edit_feed_category_rename_save"
    const val SAVE_BUTTON = "edit_feed_save"

    fun categoryChip(label: String): String =
        "edit_feed_category_${label.toE2eIdSuffix()}"
}

private fun String.toE2eIdSuffix(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') {
            char.lowercaseChar()
        } else {
            '_'
        }
    }.joinToString(separator = "")
