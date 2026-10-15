package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
) {
    var settled by remember { mutableStateOf(false) }
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestConfirm by rememberUpdatedState(onConfirm)
    val dismiss = {
        if (!settled) {
            settled = true
            latestDismiss()
        }
    }
    Dialog(title = title, onCloseRequest = dismiss, alertStyle = true) {
        ConfirmationContent(
            title = title,
            message = message,
            onConfirm = {
                if (!settled) {
                    settled = true
                    latestConfirm()
                    latestDismiss()
                }
            },
            onDismiss = dismiss,
            modifier = Modifier.fillMaxSize(),
            enabled = !settled,
            confirmLabel = confirmLabel,
        )
    }
}

@Composable
internal fun ConfirmationContent(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    confirmLabel: String? = null,
) {
    val strings = LocalFeedFlowStrings.current
    val cancelFocus = remember { FocusRequester() }
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    Column(
        modifier = modifier.padding(DesktopDesignTokens.spacePanel),
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceLarge),
        ) {
            Text(
                title,
                style = DesktopDesignTokens.sectionHeading,
                modifier = Modifier.testTag("confirmation-title"),
            )
            Text(message)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                enabled = enabled,
                modifier = Modifier.heightIn(min = actionHeight).focusRequester(cancelFocus),
            ) {
                Text(strings.cancelButton)
            }
            DefaultButton(onClick = onConfirm, enabled = enabled, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(confirmLabel ?: strings.confirmButton)
            }
        }
    }
}
