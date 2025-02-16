package com.prof18.feedflow.android

import com.prof18.feedflow.core.model.LinkOpeningPreference
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Settings

@Serializable
data object AddFeed

@Serializable
data object FeedList

@Serializable
data object About

@Serializable
data object Licenses

@Serializable
data object ImportExport

@Serializable
data object ReaderMode

@Serializable
data object Search

@Serializable
data object Accounts

@Serializable
data object FreshRssSync

@Serializable
data class EditFeed(
    val id: String,
    val url: String,
    val title: String,
    val categoryId: String?,
    val categoryTitle: String?,
    val lastSyncTimestamp: Long?,
    val logoUrl: String?,
    val linkOpeningPreference: LinkOpeningPreference,
    val isHidden: Boolean,
    val isPinned: Boolean,
)
