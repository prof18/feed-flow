package com.prof18.feedflow.desktop.main

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.prof18.feedflow.desktop.ui.components.desktopSearchShortcuts
import org.jetbrains.jewel.foundation.theme.JewelTheme

@Composable
internal fun MainWindowSurface(
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val shortcutModifier = if (onSearch != null) {
        Modifier.desktopSearchShortcuts(onSearch).focusRequester(focusRequester).focusable()
    } else {
        Modifier
    }
    LaunchedEffect(onSearch != null) {
        if (onSearch != null) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier = modifier.then(shortcutModifier).background(JewelTheme.globalColors.panelBackground),
        content = content,
    )
}
