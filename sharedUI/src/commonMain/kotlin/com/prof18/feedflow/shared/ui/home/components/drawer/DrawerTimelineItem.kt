package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

@Composable
internal fun DrawerTimelineItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItem: DrawerItem.Timeline,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Timeline,
        badge = if (drawerItem.unreadCount > 0) {
            {
                Text(
                    text = drawerItem.unreadCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            null
        },
        label = {
            Text(
                text = LocalFeedFlowStrings.current.drawerTitleTimeline,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Feed,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onFeedFilterSelected(FeedFilter.Timeline)
        },
    )
}

@Preview
@Composable
private fun DrawerTimelineItemPreview() {
    PreviewHelper {
        DrawerTimelineItem(
            currentFeedFilter = FeedFilter.Timeline,
            onFeedFilterSelected = {},
            drawerItem = DrawerItem.Timeline(unreadCount = 10),
        )
    }
}
