package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

private const val ScrimAlpha = 0.32f

@Composable
internal fun DesktopLoadingOperationOverlay(feedOperation: FeedOperation) {
    val strings = LocalFeedFlowStrings.current
    val title = when (feedOperation) {
        FeedOperation.Deleting -> strings.deletingFeedDialogTitle
        FeedOperation.MarkingAllRead -> strings.markingAllReadDialogTitle
        FeedOperation.None -> return
    }
    val windowSize = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.toSize().toDpSize() }
    val focusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(DesktopDesignTokens.selectionCornerSize)
    Popup(popupPositionProvider = WindowOriginPosition, properties = PopupProperties(focusable = true)) {
        Box(
            modifier = Modifier
                .size(windowSize)
                .focusRequester(focusRequester)
                .focusable()
                // The operation owns the window until it ends: nothing dismisses it and nothing behind it reacts.
                .onPreviewKeyEvent { true }
                .pointerInput(Unit) { detectTapGestures { } }
                .background(Color.Black.copy(alpha = ScrimAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .background(JewelTheme.globalColors.panelBackground, shape)
                    .border(width = 1.dp, color = JewelTheme.globalColors.borders.normal, shape = shape)
                    .padding(
                        horizontal = DesktopDesignTokens.spacePanel,
                        vertical = DesktopDesignTokens.spaceSection,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = DesktopDesignTokens.spaceLarge),
            ) {
                CircularProgressIndicator()
                Text(title)
            }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

private object WindowOriginPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

@Composable
internal fun ReaderEmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.AutoMirrored.Outlined.Article),
            contentDescription = null,
            tint = JewelTheme.globalColors.text.info.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp).size(48.dp),
        )
        Text(
            LocalFeedFlowStrings.current.selectArticleToRead,
            color = JewelTheme.globalColors.text.info,
            fontSize = 16.sp,
        )
    }
}
