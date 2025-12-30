package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun MenuBarScope.FeedMenu(
    isMacOS: Boolean,
    state: MenuBarState,
    callbacks: FeedMenuCallbacks,
) {
    Menu(LocalFeedFlowStrings.current.settingsTitleFeed, mnemonic = 'E') {
        Item(
            text = LocalFeedFlowStrings.current.addFeed,
            onClick = callbacks.onAddFeed,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.N, meta = true)
            } else {
                KeyShortcut(Key.N, ctrl = true)
            },
        )

        if (state.feedFilter is FeedFilter.Source) {
            Item(
                text = LocalFeedFlowStrings.current.editFeed,
                onClick = { callbacks.onEditFeed(state.feedFilter.feedSource) },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.E, meta = true)
                } else {
                    KeyShortcut(Key.E, ctrl = true)
                },
            )
            Separator()
        }

        Item(
            text = LocalFeedFlowStrings.current.feedsTitle,
            onClick = callbacks.onFeedsClick,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.L, meta = true)
            } else {
                KeyShortcut(Key.L, ctrl = true)
            },
        )

        Separator()

        Item(
            text = LocalFeedFlowStrings.current.settingsBlockedWords,
            onClick = callbacks.onBlockedWordsClick,
        )
    }
}

internal data class FeedMenuCallbacks(
    val onAddFeed: () -> Unit,
    val onEditFeed: (FeedSource) -> Unit,
    val onFeedsClick: () -> Unit,
    val onBlockedWordsClick: () -> Unit,
)
