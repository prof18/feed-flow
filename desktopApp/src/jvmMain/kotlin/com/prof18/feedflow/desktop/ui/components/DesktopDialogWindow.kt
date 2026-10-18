package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.rememberDialogState
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.ui.DesktopWindowBackground
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import java.awt.Dimension
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

internal val MacToolbarTitleAreaHeight = 30.dp

// Height a window's content must leave free at the top when it extends behind the macOS title bar.
@Suppress("CompositionLocalAllowlist")
internal val LocalWindowTopInset = staticCompositionLocalOf { 0.dp }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DesktopDialogWindow(
    title: String,
    size: DpSize,
    onCloseRequest: () -> Unit,
    minimumSize: DpSize? = null,
    resizable: Boolean = true,
    extendBehindTitleBar: Boolean = false,
    alertStyle: Boolean = false,
    activationRequest: Int = 0,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable DialogWindowScope.(Modifier) -> Unit,
) {
    val dialogState = rememberDialogState(size = size)
    val desktopOs = remember { getDesktopOS() }
    val isMacOs = desktopOs.isMacOs()
    // Alerts show their question once, as the heading inside the content. macOS alerts have no visible title, so
    // the drawn title is skipped (the window keeps it for VoiceOver and the Window menu); Windows and Linux show
    // the app name in the native title bar, as their confirmation dialogs do.
    val windowTitle = nativeWindowTitle(title, alertStyle, isMacOs, LocalFeedFlowStrings.current.appName)
    val drawnTitle = title.takeUnless { alertStyle }

    SwingDialog(
        state = dialogState,
        title = windowTitle,
        onCloseRequest = onCloseRequest,
        resizable = resizable,
        init = { dialog -> dialog.restoreInitialLinuxWindowSize(dialogState.size) },
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = { event ->
            when {
                onKeyEvent(event) -> true
                event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                    onCloseRequest()
                    true
                }
                else -> false
            }
        },
    ) {
        DesktopWindowBackground(window, java.awt.Color(JewelTheme.globalColors.panelBackground.toArgb()))
        LaunchedEffect(window, activationRequest) {
            if (activationRequest > 0) {
                SwingUtilities.invokeLater {
                    if (window.isShowing) {
                        window.toFront()
                        window.requestFocus()
                    }
                }
            }
        }
        val density = LocalDensity.current
        LaunchedEffect(window, minimumSize, density.density) {
            SwingUtilities.invokeLater {
                minimumSize?.let { minSize ->
                    val transform = window.graphicsConfiguration.defaultTransform
                    window.minimumSize = Dimension(
                        (minSize.width.value * density.density / transform.scaleX).roundToInt(),
                        (minSize.height.value * density.density / transform.scaleY).roundToInt(),
                    )
                }
                if (isMacOs) {
                    with(window.rootPane) {
                        putClientProperty("apple.awt.transparentTitleBar", true)
                        putClientProperty("apple.awt.fullWindowContent", true)
                        putClientProperty("apple.awt.windowTitleVisible", false)
                    }
                }
            }
        }

        val windowBackground = JewelTheme.globalColors.panelBackground
        val windowFocus = remember { FocusRequester() }
        val isWindowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(isWindowFocused) {
            if (isWindowFocused) windowFocus.restoreFocusedChild()
        }
        val windowModifier = Modifier
            .fillMaxSize()
            .background(windowBackground)
            .focusRequester(windowFocus)
            .onFocusedBoundsChanged { if (it != null) windowFocus.saveFocusedChild() }
            .focusGroup()
        if (isMacOs && extendBehindTitleBar) {
            Box(
                modifier = windowModifier,
            ) {
                CompositionLocalProvider(LocalWindowTopInset provides MacToolbarTitleAreaHeight) {
                    content(Modifier.fillMaxSize())
                }
                MacTitleArea(drawnTitle)
            }
        } else {
            Column(
                modifier = windowModifier,
            ) {
                if (isMacOs) MacTitleArea(drawnTitle)
                content(Modifier.fillMaxSize())
            }
        }
    }
}

// The strip under the macOS traffic lights; it stays empty for alerts, which show their question in the content.
@Composable
private fun MacTitleArea(title: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MacToolbarTitleAreaHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

internal fun nativeWindowTitle(title: String, alertStyle: Boolean, isMacOs: Boolean, appName: String): String =
    if (alertStyle && !isMacOs) appName else title
