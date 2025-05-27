package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.shared.ui.home.components.FeedItemView
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun FeedListFontSettings(
    fontSizes: FeedFontSizes,
    modifier: Modifier = Modifier,
    isHideDescriptionEnabled: Boolean,
    isHideImagesEnabled: Boolean,
    dateFormat: DateFormat,
    updateFontScale: (Int) -> Unit,
) {
    val color = if (isSystemInDarkTheme()) {
        Color(0xFF333439)
    } else {
        Color(0xFFE2E2E9)
    }
    Column {
        Box(
            modifier = modifier
                .background(color)
                .padding(Spacing.medium),
        ) {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
            ) {
                FeedItemView(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background),
                    feedItem = FeedItem(
                        id = "1",
                        url = "https://www.example.com",
                        title = LocalFeedFlowStrings.current.settingsFontScaleTitleExample,
                        subtitle = if (isHideDescriptionEnabled) {
                            null
                        } else {
                            LocalFeedFlowStrings.current.settingsFontScaleSubtitleExample
                        },
                        content = null,
                        imageUrl = if (isHideImagesEnabled) null else "https://lipsum.app/200x200",
                        feedSource = FeedSource(
                            id = "1",
                            url = "https://www.example.it",
                            title = LocalFeedFlowStrings.current.settingsFontScaleFeedSourceExample,
                            category = null,
                            lastSyncTimestamp = null,
                            logoUrl = null,
                            linkOpeningPreference = LinkOpeningPreference.DEFAULT,
                            isHiddenFromTimeline = false,
                            isPinned = false,
                            isNotificationEnabled = false,
                        ),
                        pubDateMillis = null,
                        dateString = when(dateFormat) {
                            DateFormat.NORMAL -> "25/12"
                            DateFormat.AMERICAN -> "12/25"
                        },
                        commentsUrl = null,
                        isRead = false,
                        isBookmarked = false,
                    ),
                    feedFontSize = fontSizes,
                    shareCommentsMenuLabel = LocalFeedFlowStrings.current.menuShareComments,
                    shareMenuLabel = LocalFeedFlowStrings.current.menuShare,
                    disableClick = true,
                    onFeedItemClick = {},
                    onBookmarkClick = { _, _ -> },
                    onReadStatusClick = { _, _ -> },
                    onCommentClick = {},
                    onShareClick = {},
                )
            }
        }

        // 16 default
        // 12 min ( -4 )
        // 32 max ( +16 )
        Text(
            text = LocalFeedFlowStrings.current.settingsFeedListScaleTitle,
            modifier = Modifier.padding(Spacing.regular),
            style = MaterialTheme.typography.bodyLarge,
        )

        SliderWithPlusMinus(
            modifier = Modifier.padding(horizontal = Spacing.regular),
            value = fontSizes.scaleFactor.toFloat(),
            onValueChange = {
                updateFontScale(it.toInt())
            },
            valueRange = -4f..16f,
            steps = 20,
        )
    }
}
