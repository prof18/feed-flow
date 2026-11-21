package com.prof18.feedflow.desktop.window

import com.prof18.feedflow.shared.data.DesktopWindowSettingsRepository
import com.prof18.winkeep.SavedPlacement
import com.prof18.winkeep.WindowBounds
import com.prof18.winkeep.WindowOrigin
import com.prof18.winkeep.WindowSnapshot
import com.prof18.winkeep.WindowStateStore
import kotlin.math.roundToInt

/**
 * Keeps the main window geometry in the `DESKTOP_WINDOW_*` preferences FeedFlow used before WinKeep,
 * so existing users keep their window and an older build still reads what this one writes.
 *
 * The legacy keys have no room for the monitor, so snapshots never carry one: WinKeep then restores
 * on the monitor that overlaps the saved bounds most. The keys are written one by one, so a crash
 * mid-save can leave a mix of old and new values; WinKeep still fits whatever it loads on screen.
 */
internal class FeedFlowWindowStateStore(
    private val repository: DesktopWindowSettingsRepository,
) : WindowStateStore {

    override suspend fun load(): WindowSnapshot? {
        if (!repository.hasSavedWindowState()) return null
        val x = repository.getDesktopWindowXPositionDp()
        val y = repository.getDesktopWindowYPositionDp()
        val position = if (x != null && y != null) WindowOrigin(x.toDouble(), y.toDouble()) else null
        // WindowBounds rejects an unusable size; WinKeep reports it and opens the default size instead.
        val bounds = WindowBounds(
            position = position,
            width = repository.getDesktopWindowWidthDp().toDouble(),
            height = repository.getDesktopWindowHeightDp().toDouble(),
        )
        return WindowSnapshot(normalBounds = bounds, placement = savedPlacement(repository.getDesktopWindowPlacement()))
    }

    override suspend fun save(snapshot: WindowSnapshot) {
        val bounds = snapshot.normalBounds
        repository.setDesktopWindowWidthDp(bounds.width.roundToInt())
        repository.setDesktopWindowHeightDp(bounds.height.roundToInt())
        val position = bounds.position
        if (position != null) {
            repository.setDesktopWindowXPositionDp(position.x.toFloat())
            repository.setDesktopWindowYPositionDp(position.y.toFloat())
        } else {
            repository.removeDesktopWindowPosition()
        }
        repository.setDesktopWindowPlacement(snapshot.placement.legacyName())
    }

    override suspend fun flush() {
        repository.flushDesktopWindowState()
    }

    override suspend fun clear() {
        repository.clearDesktopWindowState()
        repository.flushDesktopWindowState()
    }
}

// The legacy values are Compose WindowPlacement names; anything else, minimized included, opens floating.
private fun savedPlacement(legacyName: String?): SavedPlacement = when (legacyName) {
    "Maximized" -> SavedPlacement.Maximized
    "Fullscreen" -> SavedPlacement.Fullscreen
    else -> SavedPlacement.Floating
}

private fun SavedPlacement.legacyName(): String = when (this) {
    SavedPlacement.Floating -> "Floating"
    SavedPlacement.Maximized -> "Maximized"
    SavedPlacement.Fullscreen -> "Fullscreen"
}
