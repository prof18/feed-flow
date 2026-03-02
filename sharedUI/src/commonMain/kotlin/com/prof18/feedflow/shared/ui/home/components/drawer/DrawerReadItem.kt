package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

@Composable
internal fun DrawerReadItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItemVisualStyle: DrawerItemVisualStyle,
) {
    NavigationDrawerItem(
        selected = currentFeedFilter is FeedFilter.Read,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.drawerTitleRead,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                contentDescription = null,
            )
        },
        shape = drawerItemVisualStyle.itemShape,
        colors = drawerItemColors(drawerItemVisualStyle),
        onClick = {
            onFeedFilterSelected(FeedFilter.Read)
        },
    )
}

@Preview
@Composable
private fun DrawerReadItemPreview() {
    PreviewHelper {
        DrawerReadItem(
            currentFeedFilter = FeedFilter.Read,
            onFeedFilterSelected = {},
            drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
        )
    }
}
