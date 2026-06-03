package com.prof18.feedflow.shared.ui.feedsuggestions

object FeedSuggestionsE2eIds {
    const val DRAWER_ITEM = "feed_suggestions_drawer_item"
    const val ADD_OPTIONS_BUTTON = "feed_suggestions_add_options_button"
    const val ADD_OPTIONS_ITEM = "feed_suggestions_add_options_item"
    const val EMPTY_STATE_BUTTON = "feed_suggestions_empty_state_button"
    const val EMPTY_STATE_OPTIONS_BUTTON = "feed_suggestions_empty_state_options_button"
    const val SCREEN = "feed_suggestions_screen"

    fun category(categoryId: String): String =
        "feed_suggestions_category_${categoryId.toE2eIdSuffix()}"

    fun row(feedUrl: String): String =
        "feed_suggestions_row_${feedUrl.toE2eIdSuffix()}"

    fun addButton(feedUrl: String): String =
        "feed_suggestions_add_${feedUrl.toE2eIdSuffix()}"
}

private fun String.toE2eIdSuffix(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') {
            char
        } else {
            '_'
        }
    }.joinToString(separator = "")
