package com.prof18.feedflow.android.addfeed

import androidx.navigation3.runtime.NavKey
import com.prof18.feedflow.android.AddFeed

/**
 * Opens Add feed pre-filled with [feedUrl]. An Add feed screen already on top is replaced, so a repeated link
 * fills the field again even after the user edited or cleared it.
 */
internal fun MutableList<NavKey>.openAddFeedLink(feedUrl: String, requestId: Long) {
    if (lastOrNull() is AddFeed) removeAt(lastIndex)
    add(AddFeed(initialUrl = feedUrl, requestId = requestId))
}
