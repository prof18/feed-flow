package com.prof18.feedflow.utils

sealed class AppEnvironment {
    object Debug : AppEnvironment()
    object Release : AppEnvironment()

    fun isDebug(): Boolean =
        this is Debug

    fun isRelease(): Boolean =
        this is Release
}
