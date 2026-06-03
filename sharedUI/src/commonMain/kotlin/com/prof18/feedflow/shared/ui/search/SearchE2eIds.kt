package com.prof18.feedflow.shared.ui.search

import com.prof18.feedflow.core.model.SearchFilter

object SearchE2eIds {
    const val SEARCH_FIELD = "search_query_field"
    const val BACK_BUTTON = "search_back_button"
    const val CLEAR_BUTTON = "search_clear_button"

    fun filter(filter: SearchFilter): String =
        when (filter) {
            SearchFilter.CurrentFeed -> "search_filter_current_feed"
            SearchFilter.All -> "search_filter_all"
            SearchFilter.Read -> "search_filter_read"
            SearchFilter.Bookmarks -> "search_filter_bookmarks"
        }
}
