package com.prof18.feedflow.android.home.drawer

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewHelper

@Composable
internal fun DrawerAddItem(
    onAddFeedClicked: () -> Unit,
    drawerItemVisualStyle: DrawerItemVisualStyle,
) {
    NavigationDrawerItem(
        modifier = Modifier
            .padding(vertical = drawerItemVisualStyle.itemVerticalPadding)
            .height(drawerItemVisualStyle.itemMinHeight),
        selected = false,
        label = {
            Text(
                text = LocalFeedFlowStrings.current.addFeed,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = null,
            )
        },
        shape = drawerItemVisualStyle.itemShape,
        colors = drawerItemColors(drawerItemVisualStyle),
        onClick = {
            onAddFeedClicked()
        },
    )
}

@Preview
@Composable
private fun DrawerAddItemPreview() {
    PreviewHelper {
        DrawerAddItem(
            onAddFeedClicked = {},
            drawerItemVisualStyle = DefaultDrawerItemVisualStyle,
        )
    }
}
