package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DrawerTimelineItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItem: DrawerItem.Timeline,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    modifier: Modifier = Modifier,
) {
    DrawerItem(
        modifier = modifier,
        label = LocalFeedFlowStrings.current.drawerTitleTimeline,
        icon = { Icon(AllIconsKeys.Actions.ListFiles, null) },
        selected = currentFeedFilter is FeedFilter.Timeline,
        visualStyle = drawerItemVisualStyle,
        onClick = { onFeedFilterSelected(FeedFilter.Timeline) },
        badge = drawerItem.unreadCount.takeIf { it > 0 }?.toString(),
    )
}
