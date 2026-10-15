package com.prof18.feedflow.desktop.ui.components

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.isSpecified
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isLinux
import java.awt.Insets
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.math.roundToInt

internal fun Window.restoreInitialLinuxWindowSize(
    initialSize: DpSize,
    canRestoreSize: () -> Boolean = { true },
) {
    if (!getDesktopOS().isLinux() || !initialSize.width.isSpecified || !initialSize.height.isSpecified) return
    InitialLinuxWindowSizeObserver(
        window = this,
        initialWidth = initialSize.width.value.roundToInt().coerceAtLeast(0),
        initialHeight = initialSize.height.value.roundToInt().coerceAtLeast(0),
        canRestoreSize = canRestoreSize,
    ).attach()
}

private class InitialLinuxWindowSizeObserver(
    private val window: Window,
    private val initialWidth: Int,
    private val initialHeight: Int,
    private val canRestoreSize: () -> Boolean,
) : WindowAdapter() {
    private var opened = false
    private var packedInsets: Insets? = null
    private val resizeListener = object : ComponentAdapter() {
        override fun componentResized(event: ComponentEvent) {
            if (!opened) {
                if (packedInsets == null) packedInsets = window.insets
            }
            if (window.width == initialWidth && window.height == initialHeight) return
            val before = packedInsets
            val after = window.insets
            // A packed frame can receive its corrected X11 insets after windowOpened.
            val isInsetCorrection = before != null && before != after &&
                window.width == initialWidth + after.left + after.right - before.left - before.right &&
                window.height == initialHeight + after.top + after.bottom - before.top - before.bottom
            if (isInsetCorrection || opened) finish(restoreSize = isInsetCorrection)
        }
    }

    fun attach() {
        window.addWindowListener(this)
        window.addComponentListener(resizeListener)
    }

    override fun windowOpened(event: WindowEvent) {
        opened = true
        if (packedInsets == null) packedInsets = window.insets
        if (!canRestoreSize()) {
            finish(restoreSize = false)
        } else if (window.width != initialWidth || window.height != initialHeight) {
            finish(restoreSize = true)
        }
    }

    override fun windowClosed(event: WindowEvent) {
        finish(restoreSize = false)
    }

    private fun finish(restoreSize: Boolean) {
        window.removeWindowListener(this)
        window.removeComponentListener(resizeListener)
        if (restoreSize && canRestoreSize()) window.setSize(initialWidth, initialHeight)
    }
}
