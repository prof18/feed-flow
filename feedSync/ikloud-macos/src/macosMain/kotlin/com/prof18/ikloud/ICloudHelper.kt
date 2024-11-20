package com.prof18.ikloud

import com.prof18.jni.JNIEnvVar
import com.prof18.jni.JNI_TRUE
import com.prof18.jni.jboolean
import com.prof18.jni.jclass
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileManagerItemReplacementUsingNewMetadataOnly
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
0 -> success
1 -> iCloud folder URL null
2 -> upload error
 */
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_feedsync_ICloudNativeBridge_uploadToICloud")
fun uploadToICloud(env: CPointer<JNIEnvVar>, clazz: jclass, isDebug: jboolean): Int {
    val isDebugBool = isDebug == JNI_TRUE.toUByte()
    val databasePath = getDatabaseUrl(isDebugBool)
    val iCloudUrl = getICloudFolderURL(isDebugBool) ?: return 1
    memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()

        // Copy doesn't override the item, so we need to clear it before.
        // An alternative would be checking the existence of the file before and copy or replace.
        NSFileManager.defaultManager.removeItemAtURL(
            iCloudUrl,
            null,
        )

        NSFileManager.defaultManager.copyItemAtURL(
            srcURL = databasePath,
            toURL = iCloudUrl,
            error = errorPtr.ptr,
        )

        if (errorPtr.value != null) {
            return 2
        }

        return 0
    }
}

/**
0 -> success
1 -> url null
2 -> temp url null
3 -> download error
 4 -> database replace error
 */
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_feedsync_ICloudNativeBridge_iCloudDownload")
fun iCloudDownload(env: CPointer<JNIEnvVar>, clazz: jclass, isDebug: jboolean): Int {
    val isDebugBool = isDebug == JNI_TRUE.toUByte()
    val iCloudUrl = getICloudFolderURL(isDebugBool) ?: return 1
    val tempUrl = getTemporaryFileUrl(isDebugBool) ?: return 2
    NSFileManager.defaultManager.removeItemAtURL(
        tempUrl,
        null,
    )

    memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()

        NSFileManager.defaultManager.copyItemAtURL(
            srcURL = iCloudUrl,
            toURL = tempUrl,
            error = errorPtr.ptr,
        )

        if (errorPtr.value != null) {
            return 3
        }

        val result = replaceDatabase(tempUrl, isDebugBool)
        return if (result) {
            0
        } else {
            4
        }
    }
}

private fun replaceDatabase(url: NSURL, isDebug: Boolean): Boolean {
    val dbUrl = getDatabaseUrl(isDebug)
    // Replace the database
    memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()
        NSFileManager.defaultManager.replaceItemAtURL(
            originalItemURL = dbUrl,
            withItemAtURL = url,
            backupItemName = "${getDatabaseName(isDebug)}.old",
            options = NSFileManagerItemReplacementUsingNewMetadataOnly,
            error = errorPtr.ptr,
            resultingItemURL = null,
        )

        return errorPtr.value == null
    }
}

private fun getDatabaseUrl(isDebug: Boolean): NSURL {
    val path = getDataPath(isDebug)
    val databaseName = getDatabaseName(isDebug)
    val databasePath = "$path/$databaseName.db"

    return NSURL.fileURLWithPath(databasePath)
}

private fun getTemporaryFileUrl(isDebug: Boolean): NSURL? {
    val documentsDirectory: NSURL? = NSFileManager.defaultManager.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask,
    ).firstOrNull() as? NSURL?
    val databaseUrl = documentsDirectory?.URLByAppendingPathComponent(getDatabaseName(isDebug))

    return databaseUrl
}

private fun getDatabaseName(isDebug: Boolean): String {
    return if (isDebug) {
        SYNC_DATABASE_NAME_DEBUG
    } else {
        SYNC_DATABASE_NAME_PROD
    }
}

private fun getDataPath(isDebug: Boolean): String {
    val appDataPath = "${NSHomeDirectory()}/Library/Application Support/FeedFlow"
    return if (isDebug) {
        "$appDataPath-dev"
    } else {
        appDataPath
    }
}

private fun getICloudFolderURL(isDebug: Boolean): NSURL? = NSFileManager.defaultManager
    .URLForUbiquityContainerIdentifier("iCloud.com.prof18.feedflow")
    ?.URLByAppendingPathComponent("Documents")
    ?.URLByAppendingPathComponent(getDatabaseName(isDebug))

private const val SYNC_DATABASE_NAME_PROD = "FeedFlowFeedSyncDB"
private const val SYNC_DATABASE_NAME_DEBUG = "FeedFlowFeedSyncDB-debug"
