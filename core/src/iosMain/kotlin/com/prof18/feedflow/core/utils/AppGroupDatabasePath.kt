package com.prof18.feedflow.core.utils

import platform.Foundation.NSFileManager

fun getAppGroupDatabasePath(): String {
    val fileManager = NSFileManager.defaultManager()
    val containerURL = fileManager.containerURLForSecurityApplicationGroupIdentifier(IOS_APP_GROUP)
    check(containerURL != null) { "Could not access App Group container" }

    val containerPath = containerURL.path
    val directoryPath = "$containerPath/databases"

    if (!fileManager.fileExistsAtPath(directoryPath)) {
        fileManager.createDirectoryAtPath(
            directoryPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    return directoryPath
}

internal const val IOS_APP_GROUP = "group.com.prof18.feedflow"
