package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.i18n.FeedFlowStrings

/**
 * What a screen reader announces as the row state. Read, non-bookmarked rows return null
 * so they stay quiet. Ignores the "hide unread indicator" setting on purpose.
 */
fun FeedItem.accessibilityState(strings: FeedFlowStrings): String? =
    when {
        !isRead && isBookmarked -> strings.articleUnreadBookmarkedState
        !isRead -> strings.articleUnreadState
        isBookmarked -> strings.articleBookmarkedContentDescription
        else -> null
    }
