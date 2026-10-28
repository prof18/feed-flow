package com.prof18.feedflow.desktop.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.prof18.feedflow.desktop.resources.Res
import com.prof18.feedflow.desktop.resources.icon
import com.prof18.feedflow.desktop.ui.DesktopUiEnvironment
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.rememberDesktopDarkTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * A small window shown when the app cannot start normally. It renders without Koin, so it works even
 * when the dependency graph or the database is unavailable.
 */
@Composable
internal fun StartupMessageWindow(
    onCloseRequest: () -> Unit,
    activationRequests: Flow<Unit> = emptyFlow(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowState = rememberWindowState(
        size = DpSize(400.dp, 260.dp),
        position = WindowPosition.Aligned(Alignment.Center),
    )
    Window(
        onCloseRequest = onCloseRequest,
        title = "FeedFlow",
        state = windowState,
        icon = painterResource(Res.drawable.icon),
        resizable = false,
    ) {
        LaunchedEffect(window, activationRequests) {
            activationRequests.collect { window.bringToFront(windowState) }
        }
        DesktopUiEnvironment(darkTheme = rememberDesktopDarkTheme()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JewelTheme.globalColors.panelBackground)
                    .padding(DesktopDesignTokens.spacePanel),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun StartupMessage(
    message: String,
    detail: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceLarge),
    ) {
        Text(text = message)
        if (detail != null) {
            Text(text = detail, color = JewelTheme.globalColors.text.info)
        }
    }
}

@Composable
internal fun StartupActions(
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceLarge, Alignment.End),
    ) {
        content()
    }
}
