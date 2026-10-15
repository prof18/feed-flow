package com.prof18.feedflow.desktop.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

internal enum class DesktopDialogWindowDestination {
    AddFeed,
    EditFeed,
    FeedSuggestions,
    BlockedWords,
    ImportExport,
    Settings,
    Accounts,
    FeedSources,
}

internal class DesktopDialogWindowNavigator {
    var feedSourceSelection by mutableStateOf<ImmutableSet<String>>(persistentSetOf())

    private val openedWindows = mutableStateMapOf<DesktopDialogWindowDestination, Boolean>()

    fun open(destination: DesktopDialogWindowDestination) {
        openedWindows[destination] = true
    }

    fun close(destination: DesktopDialogWindowDestination) {
        openedWindows[destination] = false
    }

    fun isOpen(destination: DesktopDialogWindowDestination): Boolean =
        openedWindows[destination] == true
}

@Composable
internal fun rememberDesktopDialogWindowNavigator(): DesktopDialogWindowNavigator = remember {
    DesktopDialogWindowNavigator()
}

// The home and its native management window share the main-window navigator.
@Suppress("CompositionLocalAllowlist")
internal val LocalDesktopDialogWindowNavigator = staticCompositionLocalOf<DesktopDialogWindowNavigator> {
    error("Desktop dialog navigator is not provided")
}
