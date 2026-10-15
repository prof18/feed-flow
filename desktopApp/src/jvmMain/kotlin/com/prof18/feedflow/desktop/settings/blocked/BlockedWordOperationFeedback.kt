package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.shared.presentation.model.BlockedWordAction
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationState
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationStatus
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun BlockedWordOperationFeedback(
    operation: BlockedWordOperationState,
    hasPendingOperations: Boolean,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val currentOperation by rememberUpdatedState(operation)
    val currentBusy by rememberUpdatedState(hasPendingOperations)
    val retry by rememberUpdatedState(onRetry)
    val message = when (operation.status) {
        BlockedWordOperationStatus.Writing -> strings.blockedWordSaving
        BlockedWordOperationStatus.Refreshing -> strings.blockedWordRefreshing
        BlockedWordOperationStatus.Failed -> strings.blockedWordOperationError
        BlockedWordOperationStatus.RefreshFailed -> strings.blockedWordRefreshError
        BlockedWordOperationStatus.Applied -> if (operation.action == BlockedWordAction.Add) {
            strings.blockedWordAdded
        } else {
            strings.blockedWordRemoved
        }
    }
    Column(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
    ) {
        Tooltip(tooltip = { Text(operation.word) }) {
            Text(
                operation.word,
                color = JewelTheme.globalColors.text.info,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (operation.status.isPending()) {
            Row(
                modifier = Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
            ) {
                CircularProgressIndicator()
                Text(message)
            }
        } else {
            Text(
                message,
                modifier = Modifier.semantics { if (operation.status.canRetry()) error(message) },
                color = if (operation.status.canRetry()) {
                    JewelTheme.globalColors.text.error
                } else {
                    JewelTheme.contentColor
                },
            )
        }
        if (operation.status.canRetry()) {
            val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
            OutlinedButton(
                onClick = {
                    if (currentOperation.generation == operation.generation &&
                        currentOperation.status.canRetry() && !currentBusy
                    ) {
                        retry(currentOperation.word)
                    }
                },
                enabled = !hasPendingOperations,
                modifier = Modifier.heightIn(min = actionHeight),
            ) { Text(strings.retryButton) }
        }
    }
}

internal fun BlockedWordOperationStatus.isPending(): Boolean =
    this == BlockedWordOperationStatus.Writing || this == BlockedWordOperationStatus.Refreshing

private fun BlockedWordOperationStatus.canRetry(): Boolean =
    this == BlockedWordOperationStatus.Failed || this == BlockedWordOperationStatus.RefreshFailed

internal fun ImmutableMap<String, BlockedWordOperationState>?.hasPendingOperations(): Boolean =
    this?.values?.any { it.status.isPending() } == true
