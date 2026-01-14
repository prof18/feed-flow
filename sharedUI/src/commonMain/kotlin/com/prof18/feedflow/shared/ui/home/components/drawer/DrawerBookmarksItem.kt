package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
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
internal fun DrawerBookmarksItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItem: DrawerItem.Bookmarks,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Bookmarks,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.drawerTitleBookmarks,
            )
        },
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
        icon = {
            Icon(
                imageVector = Icons.Default.Bookmarks,
                contentDescription = null,
            )
        },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        onClick = {
            onFeedFilterSelected(FeedFilter.Bookmarks)
        },
    )
}

@Preview
@Composable
private fun DrawerBookmarksItemPreview() {
    PreviewHelper {
        DrawerBookmarksItem(
            currentFeedFilter = FeedFilter.Bookmarks,
            onFeedFilterSelected = {},
            drawerItem = DrawerItem.Bookmarks(unreadCount = 5),
        )
    }
}
