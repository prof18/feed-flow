package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DrawerBookmarksItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    modifier: Modifier = Modifier,
) {
    DrawerItem(
        modifier = modifier,
        label = LocalFeedFlowStrings.current.drawerTitleBookmarks,
        icon = { Icon(AllIconsKeys.Toolwindows.ToolWindowBookmarks, null) },
        selected = currentFeedFilter is FeedFilter.Bookmarks,
        visualStyle = drawerItemVisualStyle,
        onClick = { onFeedFilterSelected(FeedFilter.Bookmarks) },
    )
}
