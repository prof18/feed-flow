package com.prof18.feedflow.android

import kotlinx.serialization.Serializable

@Serializable
data class Home(
    val feedSourceId: String? = null,
)

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
    val linkOpeningPreference: String,
    val isHidden: Boolean,
    val isPinned: Boolean,
    val isNotificationEnabled: Boolean,
)

@Serializable
data class DeepLinkScreen(
    val feedId: String,
)

@Serializable
internal data object Notifications
