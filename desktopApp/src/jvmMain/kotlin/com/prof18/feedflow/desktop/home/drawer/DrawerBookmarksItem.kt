package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun DrawerBookmarksItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItem: DrawerItem.Bookmarks,
    drawerItemVisualStyle: DrawerItemVisualStyle,
) {
    NavigationDrawerItem(
        modifier = Modifier
            .padding(vertical = drawerItemVisualStyle.itemVerticalPadding)
            .height(drawerItemVisualStyle.itemMinHeight),
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
        shape = drawerItemVisualStyle.itemShape,
        colors = drawerItemColors(drawerItemVisualStyle),
        onClick = {
            onFeedFilterSelected(FeedFilter.Bookmarks)
        },
    )
}
