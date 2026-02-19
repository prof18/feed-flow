package com.prof18.feedflow.desktop

import androidx.navigation3.runtime.NavKey
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey

@Serializable data object Search : NavKey

@Serializable data object Accounts : NavKey

@Serializable data object FeedSuggestions : NavKey

@Serializable
data class ReaderMode(
    val id: String,
    val url: String,
    val title: String,
    val isBookmarked: Boolean,
    val linkOpeningPreference: String,
    val commentsUrl: String?,
) : NavKey

// Sync screens
@Serializable data object DropboxSync : NavKey

@Serializable data object GoogleDriveSync : NavKey

@Serializable data object ICloudSync : NavKey

@Serializable data object FreshRssSync : NavKey

@Serializable data object MinifluxSync : NavKey

@Serializable data object BazquxSync : NavKey

@Serializable data object FeedbinSync : NavKey

fun FeedItemUrlInfo.toReaderMode(): ReaderMode = ReaderMode(
    id = id,
    url = url,
    title = title ?: "",
    isBookmarked = isBookmarked,
    linkOpeningPreference = linkOpeningPreference.name,
    commentsUrl = commentsUrl,
)
