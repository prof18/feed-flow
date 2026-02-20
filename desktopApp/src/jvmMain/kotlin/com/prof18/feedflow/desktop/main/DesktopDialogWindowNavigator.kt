package com.prof18.feedflow.desktop.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember

internal enum class DesktopDialogWindowDestination {
    AddFeed,
    EditFeed,
    BlockedWords,
    ImportExport,
    Accounts,
}

internal class DesktopDialogWindowNavigator {
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
