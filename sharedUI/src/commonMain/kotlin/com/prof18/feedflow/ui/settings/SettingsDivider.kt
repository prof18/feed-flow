package com.prof18.feedflow.ui.settings

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDivider() {
    Divider(
        modifier = Modifier,
        thickness = 0.2.dp,
        color = Color.Gray,
    )
}
