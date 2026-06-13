package com.prof18.feedflow.desktop.utils

import co.touchlab.kermit.Logger
import java.io.File

private const val APP_ID = "com.prof18.feedflow"

// Java's XToolkit derives the X11 WM_CLASS from the main class, replacing dots
// with dashes. GNOME matches the running window to the desktop entry via this
// value, so it must stay in sync with the desktop app's mainClass in
// desktopApp/build.gradle.kts and with .scripts/package-appimage.sh.
private const val WM_CLASS = "com-prof18-feedflow-desktop-MainKt"

private const val ICON_SIZE_DIR = "512x512"

/**
 * AppImages are portable single-file executables that, by design, do not register
 * themselves with the desktop environment. Without an installed `.desktop` file
 * GNOME cannot map the running window to an app, so it falls back to showing the
 * raw WM_CLASS as the name and a generic icon.
 *
 * When launched from an AppImage we self-integrate on first run by writing a
 * desktop entry and icon into the user's XDG data dir, with a [WM_CLASS]-matching
 * `StartupWMClass` so GNOME shows the proper name, icon, and supports pinning.
 */
fun integrateAppImageIfNeeded() {
    if (!System.getProperty("os.name").lowercase().contains("linux")) return

    // Set by the AppImage runtime to the absolute path of the .AppImage file.
    val appImagePath = System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() } ?: return
    // Mount point of the running AppImage; used to source the bundled icon.
    val appDir = System.getenv("APPDIR")?.takeIf { it.isNotBlank() }

    try {
        writeDesktopEntry(appImagePath)
        installIcon(appDir)
    } catch (e: Exception) {
        Logger.e("Failed to integrate AppImage with the desktop", e)
    }
}

private fun xdgDataHome(): File {
    val explicit = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
    return if (explicit != null) {
        File(explicit)
    } else {
        File(System.getProperty("user.home"), ".local/share")
    }
}

private fun writeDesktopEntry(appImagePath: String) {
    val applicationsDir = File(xdgDataHome(), "applications").apply { mkdirs() }
    val desktopFile = File(applicationsDir, "$APP_ID.desktop")

    val contents = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=FeedFlow")
        appendLine("GenericName=RSS Reader")
        appendLine("Comment=A minimalistic RSS Reader")
        appendLine("Icon=$APP_ID")
        appendLine("Exec=\"$appImagePath\" %U")
        appendLine("Terminal=false")
        appendLine("StartupNotify=true")
        appendLine("Categories=Network;News;")
        appendLine("Keywords=RSS;Feed;News;Reader;")
        appendLine("StartupWMClass=$WM_CLASS")
    }

    // Rewrite only when the content differs so we keep Exec current (e.g. the
    // user moved the AppImage) without touching the file on every launch.
    if (!desktopFile.exists() || desktopFile.readText() != contents) {
        desktopFile.writeText(contents)
        desktopFile.setExecutable(true, false)
        // GNOME caches its application list; nudge it to pick up the new entry
        // sooner so the icon/name show without waiting for a shell reload.
        updateDesktopDatabase(applicationsDir)
    }
}

private fun updateDesktopDatabase(applicationsDir: File) {
    try {
        ProcessBuilder("update-desktop-database", applicationsDir.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (_: Exception) {
        // update-desktop-database may be absent; the entry is still valid on disk.
    }
}

private fun installIcon(appDir: String?) {
    val iconsDir = File(xdgDataHome(), "icons/hicolor/$ICON_SIZE_DIR/apps").apply { mkdirs() }
    val target = File(iconsDir, "$APP_ID.png")
    if (target.exists()) return

    val source = appDir?.let { dir ->
        listOf(
            File(dir, "usr/share/icons/hicolor/$ICON_SIZE_DIR/apps/$APP_ID.png"),
            File(dir, "$APP_ID.png"),
        ).firstOrNull { it.isFile }
    } ?: return

    source.copyTo(target, overwrite = true)
}
