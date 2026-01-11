package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.PreviewHelper
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier
            .padding(vertical = Spacing.regular),
        thickness = 0.2.dp,
        color = Color.Gray,
    )
}

@Preview
@Composable
private fun DrawerDividerPreview() {
    PreviewHelper {
        DrawerDivider()
    }
}
