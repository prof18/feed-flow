package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.prof18.feedflow.core.utils.DesktopOS
import com.prof18.feedflow.core.utils.getDesktopOS
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.materials.CupertinoMaterials

@Composable
@ReadOnlyComposable
fun drawerHazeStyle(): HazeStyle? = when (getDesktopOS()) {
    DesktopOS.MAC -> CupertinoMaterials.ultraThin()
    DesktopOS.WINDOWS -> null
    DesktopOS.LINUX -> null
}
