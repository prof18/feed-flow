package com.prof18.feedflow.desktop.settings.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme

// The latest Settings search jump; the id makes repeated jumps to the same row re-run.
@Suppress("CompositionLocalAllowlist")
internal val LocalSettingsSearchJump = compositionLocalOf<SettingsSearchJump?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.settingsSearchHighlight(title: String?): Modifier {
    val request = LocalSettingsSearchJump.current
    val requester = remember { BringIntoViewRequester() }
    val strength = remember { Animatable(0f) }
    val reduceMotion = LocalReduceMotion.current
    val tint = JewelTheme.globalColors.outlines.focused
    LaunchedEffect(request, title) {
        if (request == null || title == null || request.targetTitle != title) {
            strength.snapTo(0f)
            return@LaunchedEffect
        }
        // Let the freshly selected page lay out before scrolling to it.
        withFrameNanos { }
        requester.bringIntoView()
        strength.snapTo(1f)
        delay(HIGHLIGHT_HOLD_MS)
        if (reduceMotion) strength.snapTo(0f) else strength.animateTo(0f, tween(HIGHLIGHT_FADE_MS))
    }
    // Drawn over the content: section cards paint an opaque background that would hide a tint drawn behind them.
    return bringIntoViewRequester(requester).drawWithContent {
        drawContent()
        if (strength.value > 0f) drawRect(tint.copy(alpha = HIGHLIGHT_TINT_ALPHA * strength.value))
    }
}

// Runs [onJump] once per new search jump that lands on this pane, e.g. to leave a subpage.
@Composable
internal fun OnSettingsSearchJump(onJump: () -> Unit) {
    val jump = LocalSettingsSearchJump.current
    // Saveable, so a pane restored by SettingsContent's SaveableStateHolder remembers the jumps it already handled.
    // Jump ids start at 1, so 0 means "none handled"; a freshly composed pane may run onJump once, which is harmless.
    var handledId by rememberSaveable { mutableIntStateOf(0) }
    val latestOnJump by rememberUpdatedState(onJump)
    SideEffect(jump?.id) {
        if (jump != null && jump.id != handledId) {
            handledId = jump.id
            latestOnJump()
        }
    }
}

internal const val HIGHLIGHT_HOLD_MS = 1200L
internal const val HIGHLIGHT_FADE_MS = 800
private const val HIGHLIGHT_TINT_ALPHA = 0.18f

internal data class SettingsSearchJump(val id: Int, val targetTitle: String?)
