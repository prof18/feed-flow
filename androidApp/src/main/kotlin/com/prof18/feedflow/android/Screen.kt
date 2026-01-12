package com.prof18.feedflow.android

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class Home(
    val feedSourceId: String? = null,
    val categoryId: String? = null,
) : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object FeedsAndAccounts : NavKey

@Serializable
data object FeedListSettings : NavKey

@Serializable
data object ReadingBehavior : NavKey

@Serializable
data object SyncAndStorage : NavKey

@Serializable
data object WidgetSettings : NavKey

@Serializable
data object Extras : NavKey

@Serializable
data object AboutAndSupport : NavKey

@Serializable
data object AddFeed : NavKey

@Serializable
data object FeedList : NavKey

@Serializable
data object About : NavKey

@Serializable
data object Licenses : NavKey

@Serializable
data object ImportExport : NavKey

@Serializable
data object ReaderMode : NavKey

@Serializable
data object Search : NavKey

@Serializable
data object Accounts : NavKey

@Serializable
data object FreshRssSync : NavKey

@Serializable
data object MinifluxSync : NavKey

@Serializable
data object BazquxSync : NavKey

@Serializable
data object FeedbinSync : NavKey

@Serializable
data class EditFeed(
    val id: String,
    val url: String,
    val title: String,
    val categoryId: String?,
    val categoryTitle: String?,
    val lastSyncTimestamp: Long?,
    val logoUrl: String?,
    val websiteUrl: String?,
    val linkOpeningPreference: String,
    val isHidden: Boolean,
    val isPinned: Boolean,
    val isNotificationEnabled: Boolean,
    val fetchFailed: Boolean,
) : NavKey

@Serializable
internal data object Notifications : NavKey

@Serializable
internal data object BlockedWords : NavKey

@Serializable
data object FeedSuggestions : NavKey
