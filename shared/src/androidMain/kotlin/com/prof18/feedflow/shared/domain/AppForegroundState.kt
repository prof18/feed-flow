package com.prof18.feedflow.shared.domain

class AppForegroundState {
    @Volatile
    private var isForeground = false

    fun onAppForegrounded() {
        isForeground = true
    }

    fun onAppBackgrounded() {
        isForeground = false
    }

    fun isAppInForeground(): Boolean = isForeground
}
