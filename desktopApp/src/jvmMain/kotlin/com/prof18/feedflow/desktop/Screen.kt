package com.prof18.feedflow.desktop

import androidx.navigation3.runtime.NavKey
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey

@Serializable data object Search : NavKey

@Serializable
data class ReaderMode(
    val id: String,
    val url: String,
    val title: String,
    val isBookmarked: Boolean,
    val articleOpenMode: String,
    val commentsUrl: String?,
    val imageUrl: String? = null,
    val feedSourceTitle: String? = null,
) : NavKey

fun FeedItemUrlInfo.toReaderMode(): ReaderMode = ReaderMode(
    id = id,
    url = url,
    title = title ?: "",
    isBookmarked = isBookmarked,
    articleOpenMode = articleOpenMode.name,
    commentsUrl = commentsUrl,
    imageUrl = imageUrl,
    feedSourceTitle = feedSourceTitle,
)
