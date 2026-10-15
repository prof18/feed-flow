package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.runtime.Composable
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DrawerAddItem(onAddFeedClicked: () -> Unit, drawerItemVisualStyle: DrawerItemVisualStyle) {
    DrawerItem(
        LocalFeedFlowStrings.current.addFeed,
        { Icon(AllIconsKeys.General.Add, null) },
        false,
        drawerItemVisualStyle,
        onAddFeedClicked,
    )
}
