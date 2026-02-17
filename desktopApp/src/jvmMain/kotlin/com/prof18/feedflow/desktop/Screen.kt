package com.prof18.feedflow.desktop

import androidx.navigation3.runtime.NavKey
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey

@Serializable data object Search : NavKey

@Serializable data object Accounts : NavKey

@Serializable data object FeedSuggestions : NavKey

@Serializable data object AddFeed : NavKey

@Serializable data object ImportExport : NavKey

@Serializable
data class ReaderMode(
    val id: String,
    val url: String,
    val title: String,
    val isBookmarked: Boolean,
    val linkOpeningPreference: String,
    val commentsUrl: String?,
) : NavKey

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

// Sync screens
@Serializable data object DropboxSync : NavKey

@Serializable data object GoogleDriveSync : NavKey

@Serializable data object ICloudSync : NavKey

@Serializable data object FreshRssSync : NavKey

@Serializable data object MinifluxSync : NavKey

@Serializable data object BazquxSync : NavKey

@Serializable data object FeedbinSync : NavKey

// Settings
@Serializable data object BlockedWords : NavKey

@Serializable data object FeedSourceList : NavKey

// Convert domain models to/from routes
fun FeedSource.toEditFeed(): EditFeed = EditFeed(
    id = id,
    url = url,
    title = title,
    categoryId = category?.id,
    categoryTitle = category?.title,
    lastSyncTimestamp = lastSyncTimestamp,
    logoUrl = logoUrl,
    websiteUrl = websiteUrl,
    linkOpeningPreference = linkOpeningPreference.name,
    isHidden = isHiddenFromTimeline,
    isPinned = isPinned,
    isNotificationEnabled = isNotificationEnabled,
    fetchFailed = fetchFailed,
)

fun EditFeed.toFeedSource(): FeedSource = FeedSource(
    id = id,
    url = url,
    title = title,
    category = if (categoryId != null && categoryTitle != null) {
        FeedSourceCategory(categoryId, categoryTitle)
    } else {
        null
    },
    lastSyncTimestamp = lastSyncTimestamp,
    logoUrl = logoUrl,
    websiteUrl = websiteUrl,
    linkOpeningPreference = LinkOpeningPreference.valueOf(linkOpeningPreference),
    isHiddenFromTimeline = isHidden,
    isPinned = isPinned,
    isNotificationEnabled = isNotificationEnabled,
    fetchFailed = fetchFailed,
)

fun FeedItemUrlInfo.toReaderMode(): ReaderMode = ReaderMode(
    id = id,
    url = url,
    title = title ?: "",
    isBookmarked = isBookmarked,
    linkOpeningPreference = linkOpeningPreference.name,
    commentsUrl = commentsUrl,
)
