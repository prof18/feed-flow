package com.prof18.feedflow.android.ui.home.components.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem

/**
 * Faintest alpha that keeps read `onSurface` text at WCAG AA (4.5:1) on every feed background
 * of the light, dark and OLED themes. Read text always uses `onSurface`, because
 * `onSurfaceVariant` falls below 4.5:1 on the light theme at this alpha.
 */
internal const val ReadTextAlpha = 0.65f
internal const val ReadImageAlpha = 0.65f
internal const val ReadImageSaturation = 0.3f

private val readImageColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(ReadImageSaturation) })
}

/**
 * Read items are dimmed only where they mix with unread ones: the Read and Bookmarks views
 * show read items on their own, so dimming them there adds nothing.
 */
internal fun FeedItem.isDimmedAsRead(currentFeedFilter: FeedFilter): Boolean =
    isRead && currentFeedFilter !is FeedFilter.Read && currentFeedFilter !is FeedFilter.Bookmarks

internal fun readTextColor(onSurface: Color): Color = onSurface.copy(alpha = ReadTextAlpha)

@Composable
@ReadOnlyComposable
internal fun FeedItem.textColor(
    currentFeedFilter: FeedFilter,
    unreadColor: Color = MaterialTheme.colorScheme.onSurface,
): Color = if (isDimmedAsRead(currentFeedFilter)) {
    readTextColor(MaterialTheme.colorScheme.onSurface)
} else {
    unreadColor
}

internal fun FeedItem.imageAlpha(currentFeedFilter: FeedFilter): Float =
    if (isDimmedAsRead(currentFeedFilter)) ReadImageAlpha else 1f

internal fun FeedItem.imageSaturation(currentFeedFilter: FeedFilter): Float =
    if (isDimmedAsRead(currentFeedFilter)) ReadImageSaturation else 1f

internal fun FeedItem.imageColorFilter(currentFeedFilter: FeedFilter): ColorFilter? =
    if (imageSaturation(currentFeedFilter) < 1f) readImageColorFilter else null
