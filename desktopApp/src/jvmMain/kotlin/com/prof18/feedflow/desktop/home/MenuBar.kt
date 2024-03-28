package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    showDebugMenu: Boolean,
    isMarkReadWhenScrollingEnabled: Boolean,
    isShowReadItemEnabled: Boolean,
    onRefreshClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onFeedsListClick: () -> Unit,
    onClearOldFeedClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    onForceRefreshClick: () -> Unit,
    deleteFeeds: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
) {
    MenuBar {
        Menu("File", mnemonic = 'F') {
            Item(
                text = LocalFeedFlowStrings.current.refreshFeeds,
                onClick = {
                    onRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true),
            )

            Item(
                text = LocalFeedFlowStrings.current.forceFeedRefresh,
                onClick = {
                    onForceRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true, shift = true),
            )

            Item(
                text = LocalFeedFlowStrings.current.markAllReadButton,
                onClick = {
                    onMarkAllReadClick()
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.clearOldArticlesButton,
                onClick = {
                    onClearOldFeedClick()
                },
            )

            Separator()

            Item(
                text = LocalFeedFlowStrings.current.feedsTitle,
                onClick = {
                    onFeedsListClick()
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.importExportOpml,
                onClick = onImportExportClick,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
                checked = isMarkReadWhenScrollingEnabled,
                onCheckedChange = setMarkReadWhenScrolling,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
                checked = isShowReadItemEnabled,
                onCheckedChange = setShowReadItem,
            )

            Separator()

            Item(
                text = LocalFeedFlowStrings.current.reportIssueButton,
                onClick = onBugReportClick,
            )

            Separator()

            Item(
                text = LocalFeedFlowStrings.current.aboutButton,
                onClick = onAboutClick,
            )

            DebugMenu(
                showDebugMenu = showDebugMenu,
                deleteFeeds = deleteFeeds,
            )
        }
    }
}

@Composable
private fun MenuScope.DebugMenu(
    showDebugMenu: Boolean,
    deleteFeeds: () -> Unit,
) {
    if (showDebugMenu) {
        Separator()

        Item(
            text = "Delete all feeds",
            onClick = {
                deleteFeeds()
            },
        )
    }
}
