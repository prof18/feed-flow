package com.prof18.feedflow.android.readermode

internal object ReaderModeE2eIds {
    const val BACK_BUTTON = "reader_back_button"
    const val BOOKMARK_BUTTON = "reader_bookmark_button"
    const val BROWSER_BUTTON = "reader_browser_button"
    const val FONT_SIZE_BUTTON = "reader_font_size_button"
    const val TEXT_SETTINGS_SHEET = "reader_text_settings_sheet"
    const val TEXT_SETTINGS_RESET_BUTTON = "reader_text_settings_reset_button"
    const val MORE_MENU_BUTTON = "reader_more_menu_button"
    const val PREVIOUS_BUTTON = "reader_previous_button"
    const val NEXT_BUTTON = "reader_next_button"
    const val IMAGE_VIEWER = "reader_image_viewer"
    const val IMAGE_VIEWER_CLOSE_BUTTON = "reader_image_viewer_close_button"
    const val IMAGE_VIEWER_SHARE_BUTTON = "reader_image_viewer_share_button"

    fun article(feedItemId: String): String =
        "reader_article_${feedItemId.toE2eIdSuffix()}"
}

private fun String.toE2eIdSuffix(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') {
            char
        } else {
            '_'
        }
    }.joinToString(separator = "")
