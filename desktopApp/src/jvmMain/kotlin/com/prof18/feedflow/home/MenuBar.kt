package com.prof18.feedflow.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import com.prof18.feedflow.MR
import dev.icerock.moko.resources.compose.stringResource

@Suppress("LongParameterList")
@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    showDebugMenu: Boolean,
    onRefreshClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onFeedsListClick: () -> Unit,
    onClearOldFeedClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    onForceRefreshClick: () -> Unit,
    deleteFeeds: () -> Unit,
) {
    MenuBar {
        Menu("File", mnemonic = 'F') {
            Item(
                text = stringResource(resource = MR.strings.refresh_feeds),
                onClick = {
                    onRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true),
            )

            Item(
                text = stringResource(resource = MR.strings.force_feed_refresh),
                onClick = {
                    onForceRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true, shift = true),
            )

            Item(
                text = stringResource(resource = MR.strings.mark_all_read_button),
                onClick = {
                    onMarkAllReadClick()
                },
            )

            Item(
                text = stringResource(resource = MR.strings.clear_old_articles_button),
                onClick = {
                    onClearOldFeedClick()
                },
            )

            Separator()

            Item(
                text = stringResource(resource = MR.strings.feeds_title),
                onClick = {
                    onFeedsListClick()
                },
            )

            Item(
                text = stringResource(resource = MR.strings.import_export_opml),
                onClick = onImportExportClick,
            )

            Separator()

            Item(
                text = stringResource(resource = MR.strings.report_issue_button),
                onClick = onBugReportClick,
            )

            Separator()

            Item(
                text = stringResource(resource = MR.strings.about_button),
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
