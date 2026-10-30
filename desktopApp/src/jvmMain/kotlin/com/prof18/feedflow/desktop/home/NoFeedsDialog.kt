package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun NoFeedsDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onImportExportClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    onFeedSuggestionsClick: () -> Unit = {},
) {
    if (!showDialog) return
    val strings = LocalFeedFlowStrings.current
    DesktopDialogWindow(
        title = strings.noFeedModalTitle,
        size = DpSize(400.dp, 400.dp),
        onCloseRequest = onDismissRequest,
        resizable = true,
    ) { modifier ->
        NoFeedsContent(
            onDismissRequest = onDismissRequest,
            onImportExportClick = onImportExportClick,
            onAccountsClick = onAccountsClick,
            onAddFeedClick = onAddFeedClick,
            onFeedSuggestionsClick = onFeedSuggestionsClick,
            modifier = modifier,
        )
    }
}

@Composable
internal fun NoFeedsContent(
    onDismissRequest: () -> Unit,
    onImportExportClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val addFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { addFocus.requestFocus() }
    val scrollState = rememberScrollState()
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(DesktopDesignTokens.spaceSection),
            verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
        ) {
            Text(strings.noFeedModalMessage)
            DefaultButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight).focusRequester(addFocus),
                onClick = {
                    onDismissRequest()
                    onAddFeedClick()
                },
            ) { Text(strings.addFeed) }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight),
                onClick = {
                    onDismissRequest()
                    onImportExportClick()
                },
            ) { Text(strings.importExportLabel) }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight),
                onClick = {
                    onDismissRequest()
                    onAccountsClick()
                },
            ) { Text(strings.settingsAccounts) }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight),
                onClick = {
                    onDismissRequest()
                    onFeedSuggestionsClick()
                },
            ) { Text(strings.feedSuggestionsTitle) }
        }
        FeedFlowVerticalScrollbar(scrollState)
    }
}
