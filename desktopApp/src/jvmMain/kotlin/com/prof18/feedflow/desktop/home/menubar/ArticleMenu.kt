package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import com.prof18.feedflow.desktop.main.ArticleMenuCommands
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import java.awt.Frame

/** Commands on the article open in the reader; disabled while no article is open. */
@Composable
internal fun MenuBarScope.ArticleMenu(isMacOS: Boolean, article: ArticleMenuCommands?) {
    val strings = LocalFeedFlowStrings.current
    Menu(strings.menuArticle, mnemonic = 'A') {
        Item(
            text = strings.previousArticle,
            onClick = { article?.takeIf { it.canNavigatePrevious }?.onPrevious?.invoke() },
            enabled = article?.canNavigatePrevious == true,
            shortcut = shortcut(isMacOS, Key.LeftBracket),
        )
        Item(
            text = strings.nextArticle,
            onClick = { article?.takeIf { it.canNavigateNext }?.onNext?.invoke() },
            enabled = article?.canNavigateNext == true,
            shortcut = shortcut(isMacOS, Key.RightBracket),
        )
        Separator()
        Item(
            text = if (article?.isBookmarked == true) strings.menuRemoveFromBookmark else strings.menuAddToBookmark,
            onClick = { article?.onToggleBookmark?.invoke() },
            enabled = article?.onToggleBookmark != null,
            shortcut = shortcut(isMacOS, Key.D),
        )
        Item(
            text = strings.readerModeBrowserButtonContentDescription,
            onClick = { article?.onOpenInBrowser?.invoke() },
            enabled = article?.onOpenInBrowser != null,
            shortcut = shortcut(isMacOS, Key.O, shift = true),
        )
        Item(
            text = strings.menuCopyLink,
            onClick = { article?.onCopyLink?.invoke() },
            enabled = article?.onCopyLink != null,
            shortcut = shortcut(isMacOS, Key.C, shift = true),
        )
        Separator()
        Item(
            text = strings.desktopExpandReader,
            onClick = { article?.onToggleReaderFullscreen?.invoke() },
            enabled = article?.onToggleReaderFullscreen != null,
            shortcut = shortcut(isMacOS, Key.Enter, shift = true),
        )
    }
}

/** The standard macOS Window menu. */
@Composable
internal fun MenuBarScope.WindowMenu(window: ComposeWindow) {
    val strings = LocalFeedFlowStrings.current
    Menu(strings.menuWindow) {
        Item(
            text = strings.menuMinimize,
            onClick = { window.extendedState = window.extendedState or Frame.ICONIFIED },
            shortcut = KeyShortcut(Key.M, meta = true),
        )
        Item(
            text = strings.menuZoom,
            onClick = {
                val maximized = window.extendedState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH
                window.extendedState = if (maximized) Frame.NORMAL else Frame.MAXIMIZED_BOTH
            },
        )
    }
}

internal fun shortcut(isMacOS: Boolean, key: Key, shift: Boolean = false): KeyShortcut =
    if (isMacOS) KeyShortcut(key, meta = true, shift = shift) else KeyShortcut(key, ctrl = true, shift = shift)
