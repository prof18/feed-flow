package com.prof18.feedflow.desktop.main

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Commands the native menu bar triggers on the main window content. Screens register what they can do
 * while they are shown and clear it when they leave, so menu items are enabled only when meaningful.
 */
@Stable
internal class DesktopMenuCommands {
    var article by mutableStateOf<ArticleMenuCommands?>(null)
    var search by mutableStateOf<(() -> Unit)?>(null)
    var toggleSidebar by mutableStateOf<(() -> Unit)?>(null)
}

/** Actions on the article currently open in the reader. */
@Immutable
internal data class ArticleMenuCommands(
    val articleId: String,
    val isBookmarked: Boolean,
    val canNavigatePrevious: Boolean,
    val canNavigateNext: Boolean,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onToggleBookmark: (() -> Unit)?,
    val onOpenInBrowser: (() -> Unit)?,
    val onCopyLink: (() -> Unit)?,
    val onToggleReaderFullscreen: (() -> Unit)?,
)

// Secondary content and the menu bar share the main-window command holder.
@Suppress("CompositionLocalAllowlist")
internal val LocalDesktopMenuCommands = staticCompositionLocalOf { DesktopMenuCommands() }
