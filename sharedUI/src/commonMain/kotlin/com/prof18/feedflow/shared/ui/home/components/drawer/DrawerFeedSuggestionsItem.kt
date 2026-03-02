package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

@Composable
internal fun DrawerFeedSuggestionsItem(
    onFeedSuggestionsClick: () -> Unit,
    drawerItemVisualStyle: DrawerItemVisualStyle,
) {
    NavigationDrawerItem(
        modifier = Modifier
            .padding(vertical = drawerItemVisualStyle.itemVerticalPadding)
            .height(drawerItemVisualStyle.itemMinHeight),
        selected = false,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.feedSuggestionsTitle,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
            )
        },
        shape = drawerItemVisualStyle.itemShape,
        colors = drawerItemColors(drawerItemVisualStyle),
        onClick = {
            onFeedSuggestionsClick()
        },
    )
}

@Preview
@Composable
private fun DrawerFeedSuggestionsItemPreview() {
    PreviewHelper {
        DrawerFeedSuggestionsItem(
            onFeedSuggestionsClick = {},
            drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
        )
    }
}
