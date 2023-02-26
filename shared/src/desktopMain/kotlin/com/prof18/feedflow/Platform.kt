package com.prof18.feedflow

object DesktopPlatform: Platform {
    override val name: String = "Desktop!"
}

actual fun getPlatform(): Platform {
    return DesktopPlatform
}

actual fun getDateMillisFromString(dateString: String): Long? {
    return null
}