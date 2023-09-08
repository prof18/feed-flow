package com.prof18.feedflow

import com.prof18.feedflow.utils.AppEnvironment
import java.io.File

internal object AppDataPathBuilder {

    private val appDataPath = when {
        System.getProperty("os.name").contains("Mac", true) -> {
            "/Users/${System.getProperty("user.name")}/Library/Application Support/FeedFlow"
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
