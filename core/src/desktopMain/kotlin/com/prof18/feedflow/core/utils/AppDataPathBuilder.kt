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
        else -> {
            error("This type OS not implemented")
        }
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
