package com.prof18.feedflow.core.utils

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.experimental.ExperimentalObjCRefinement

class DatabaseFileMigration(
    private val databaseName: String,
) {

    @OptIn(ExperimentalForeignApi::class, ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    fun migrate() {
        if (!isDatabaseAvailable()) {
            Logger.d { "No need to migrate the $databaseName database" }
            return
        }

        val databasePathString = getAppDatabasePathAsString()
        val appDatabasePath = NSURL.fileURLWithPath(databasePathString)

        val appWalPath = NSURL.fileURLWithPath("$databasePathString-wal")
        val appShmPath = NSURL.fileURLWithPath("$databasePathString-shm")

        val groupDatabasePath = NSURL.fileURLWithPath(getAppGroupDatabasePath())
            .URLByAppendingPathComponent(databaseName) ?: return

        groupDatabasePath.path?.let { path ->
            if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
                Logger.d { "Database file already exists in the new location, skipping" }
                return
            }
        }

        val groupWalPath = NSURL.fileURLWithPath(getAppGroupDatabasePath())
            .URLByAppendingPathComponent("$databaseName-wal") ?: return

        val groupShmPath = NSURL.fileURLWithPath(getAppGroupDatabasePath())
            .URLByAppendingPathComponent("$databaseName-shm") ?: return

        // Move files
        NSFileManager.defaultManager.copyItemAtURL(
            srcURL = appDatabasePath,
            toURL = groupDatabasePath,
            error = null,
        )
        NSFileManager.defaultManager.copyItemAtURL(
            srcURL = appWalPath,
            toURL = groupWalPath,
            error = null,
        )
        NSFileManager.defaultManager.copyItemAtURL(
            srcURL = appShmPath,
            toURL = groupShmPath,
            error = null,
        )

        // Delete stuff
        NSFileManager.defaultManager.removeItemAtURL(appDatabasePath, null)
        NSFileManager.defaultManager.removeItemAtURL(appWalPath, null)
        NSFileManager.defaultManager.removeItemAtURL(appShmPath, null)

        Logger.d { "$databaseName Database file Migration done" }
    }

    private fun isDatabaseAvailable(): Boolean {
        val databaseDirectory = getAppDatabasePathAsString()
        val fileManager = NSFileManager.defaultManager()
        return fileManager.fileExistsAtPath(databaseDirectory)
    }

    private fun getAppDatabasePathAsString(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths[0] as String

        return "$documentsDirectory/databases/$databaseName"
    }
}
