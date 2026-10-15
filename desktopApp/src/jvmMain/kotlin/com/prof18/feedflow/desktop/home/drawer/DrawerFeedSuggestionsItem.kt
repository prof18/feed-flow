package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.runtime.Composable
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DrawerFeedSuggestionsItem(
    onFeedSuggestionsClick: () -> Unit,
    drawerItemVisualStyle: DrawerItemVisualStyle,
) {
    DrawerItem(
        LocalFeedFlowStrings.current.feedSuggestionsTitle,
        { Icon(AllIconsKeys.Actions.IntentionBulb, null) },
        false,
        drawerItemVisualStyle,
        onFeedSuggestionsClick,
    )
}
