package com.prof18.feedflow.desktop.startup

import java.io.File
import kotlin.io.path.createTempDirectory

// Unix socket paths are limited to about 100 bytes and the macOS temp folder is already long, so use /tmp where it
// exists. Windows has no /tmp, but its temp folder is short enough.
internal fun createShortTempDirectory(prefix: String): File {
    val shortParent = File("/tmp").takeIf { it.isDirectory }
    val directory = if (shortParent != null) {
        createTempDirectory(shortParent.toPath(), prefix)
    } else {
        createTempDirectory(prefix)
    }
    return directory.toFile()
}
