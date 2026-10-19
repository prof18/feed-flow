package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.DesktopFeedbackData
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import kotlin.math.abs

@Composable
@NonRestartableComposable
internal fun DesktopSnackbarHost(state: DesktopFeedbackState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier)
}

@Composable
internal fun SnackbarHost(state: DesktopFeedbackState, modifier: Modifier = Modifier) {
    val data = state.currentFeedback ?: return
    FeedbackContent(data, modifier)
}

@Composable
private fun FeedbackContent(data: DesktopFeedbackData, modifier: Modifier = Modifier) {
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(data, accessibilityManager) {
        val baseDuration = when (data.duration) {
            DesktopFeedbackDuration.Short -> ShortSnackbarDuration
            DesktopFeedbackDuration.Long -> LongSnackbarDuration
            DesktopFeedbackDuration.Indefinite -> return@LaunchedEffect
        }
        val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = baseDuration,
            containsIcons = false,
            containsText = true,
            containsControls = true,
        ) ?: baseDuration
        delay(timeout)
        data.dismiss()
    }
    val shape = RoundedCornerShape(6.dp)
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    var dragOffset by remember(data) { mutableFloatStateOf(0f) }
    var width by remember(data) { mutableFloatStateOf(0f) }
    FlowRow(
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(12.dp)
            .widthIn(max = 640.dp)
            .onSizeChanged { width = it.width.toFloat() }
            .graphicsLayer { translationX = dragOffset }
            .pointerInput(data) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f },
                    onDragEnd = {
                        if (width > 0f && abs(dragOffset) > width * DismissFraction) data.dismiss()
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                    },
                )
            }
            .background(JewelTheme.globalColors.panelBackground, shape)
            .border(width = 1.dp, color = JewelTheme.globalColors.borders.normal, shape = shape)
            .padding(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(data.message)
        data.actionLabel?.let { label ->
            OutlinedButton(onClick = data::performAction, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(label)
            }
        }
        OutlinedButton(onClick = data::dismiss, modifier = Modifier.heightIn(min = actionHeight)) {
            Text(LocalFeedFlowStrings.current.closeButtonContentDescription)
        }
    }
}

private const val ShortSnackbarDuration = 4_000L
private const val LongSnackbarDuration = 10_000L
private const val DismissFraction = 0.25f
