package com.prof18.feedflow

import java.io.File

internal object AppDataPathBuilder {

    private val appDataPath = when {
        System.getProperty("os.name").contains("Mac", true) -> {
            "/Users/${System.getProperty("user.name")}/Library/Application Support/FeedFlow"
        }
        System.getProperty("os.name").contains("windows", true) -> {
            "${System.getProperty("user.home")}\\AppData\\Local\\FeedFlow"
        }
        // TODO: Add linux!
        else -> {
            error("This type OS not implemented")
        }
    }

    fun getAppDataPath(): String {
        val appPath = appDataPath
        if (!File(appPath).exists()) {
            File(appPath).mkdirs()
        }
        return appPath
    }
}
