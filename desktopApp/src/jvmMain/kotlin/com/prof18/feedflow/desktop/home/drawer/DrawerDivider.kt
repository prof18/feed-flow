package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import org.jetbrains.jewel.foundation.theme.JewelTheme

@Composable
internal fun DrawerDivider(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier.fillMaxWidth().padding(vertical = DesktopDesignTokens.spaceMedium).height(1.dp)
            .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.55f)),
    )
}
