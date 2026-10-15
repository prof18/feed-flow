package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DrawerReadItem(
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    modifier: Modifier = Modifier,
) {
    DrawerItem(
        LocalFeedFlowStrings.current.drawerTitleRead,
        { Icon(AllIconsKeys.Actions.Checked, null) },
        currentFeedFilter is FeedFilter.Read,
        drawerItemVisualStyle,
        { onFeedFilterSelected(FeedFilter.Read) },
        modifier = modifier,
    )
}
