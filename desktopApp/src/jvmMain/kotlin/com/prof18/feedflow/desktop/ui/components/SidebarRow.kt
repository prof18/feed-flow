package com.prof18.feedflow.desktop.ui.components

import com.prof18.feedflow.core.model.FeedFilter

internal data class SidebarRow(
    val entry: SidebarEntry,
    val filter: FeedFilter,
    val section: String? = null,
    val parentSection: String? = null,
    val unreadCount: Long? = null,
)
