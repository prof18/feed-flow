package com.prof18.feedflow.core.utils

enum class DesktopOS {
    WINDOWS,
    MAC,
    LINUX,
}

fun getDesktopOS(): DesktopOS {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> DesktopOS.WINDOWS
        osName.contains("mac") -> DesktopOS.MAC
        else -> DesktopOS.LINUX
    }
}
