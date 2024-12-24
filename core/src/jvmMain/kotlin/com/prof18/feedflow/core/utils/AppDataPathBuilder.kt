package com.prof18.feedflow.core.utils

import java.io.File

object AppDataPathBuilder {

    private val appDataPath = when {
        System.getProperty("os.name").contains("Mac", true) -> {
            "${System.getProperty("user.home")}/Library/Application Support/FeedFlow"
        }
        System.getProperty("os.name").contains("windows", true) -> {
            "${System.getProperty("user.home")}\\AppData\\Local\\FeedFlow"
        }
        else -> System.getenv("XDG_DATA_HOME")?.let { "$it/FeedFlow" }
            ?: "${System.getProperty("user.home")}/.local/share/FeedFlow"
    }

    fun getAppDataPath(appEnvironment: AppEnvironment): String {
        val appPath = if (appEnvironment.isDebug()) {
            "$appDataPath-dev"
        } else {
            appDataPath
        }
        if (!File(appPath).exists()) {
            File(appPath).mkdirs()
        }
        return appPath
    }
}
