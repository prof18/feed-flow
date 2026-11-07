package com.prof18.feedflow.desktop.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds a feed URL from an add-feed link until the main window opens the Add feed dialog. */
internal class AddFeedLinkInbox {
    private val mutableLinks = MutableStateFlow<String?>(null)
    val links: StateFlow<String?> = mutableLinks.asStateFlow()

    fun offer(feedUrl: String) {
        mutableLinks.value = feedUrl
    }

    /** Clears [feedUrl] only if no newer link replaced it in the meantime. */
    fun consume(feedUrl: String) {
        mutableLinks.compareAndSet(feedUrl, null)
    }
}

internal val addFeedLinkInbox = AddFeedLinkInbox()
