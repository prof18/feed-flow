package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.ui.home.components.list.FeedItemContainer
import com.prof18.feedflow.shared.ui.home.components.list.FeedItemView
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun FeedListFontSettings(
    fontSizes: FeedFontSizes,
    feedLayout: FeedLayout,
    isHideDescriptionEnabled: Boolean,
    isHideImagesEnabled: Boolean,
    isHideDateEnabled: Boolean,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    updateFontScale: (Int) -> Unit,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(Spacing.medium),
        ) {
            FeedItemContainer(feedLayout) {
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
                            websiteUrl = null,
                            linkOpeningPreference = LinkOpeningPreference.DEFAULT,
                            isHiddenFromTimeline = false,
                            isPinned = false,
                            isNotificationEnabled = false,
                            fetchFailed = false,
                        ),
                        pubDateMillis = null,
                        dateString = if (isHideDateEnabled) {
                            null
                        } else {
                            val datePart = when (dateFormat) {
                                DateFormat.NORMAL -> "25/12"
                                DateFormat.AMERICAN -> "12/25"
                            }
                            val timePart = when (timeFormat) {
                                TimeFormat.HOURS_24 -> "14:30"
                                TimeFormat.HOURS_12 -> "2:30 PM"
                            }
                            "$datePart - $timePart"
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
                    onOpenFeedSettings = {},
                    feedLayout = feedLayout,
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
