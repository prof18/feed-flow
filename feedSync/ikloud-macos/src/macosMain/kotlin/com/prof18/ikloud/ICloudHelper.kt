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
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSFileReadNoSuchFileError
import platform.Foundation.NSUUID
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
    return uploadToICloud(databasePath, iCloudUrl)
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
    return iCloudDownload(
        iCloudUrl = iCloudUrl,
        tempUrl = tempUrl,
        databaseUrl = getDatabaseUrl(isDebugBool),
        databaseName = getDatabaseName(isDebugBool),
    )
}

internal fun uploadToICloud(databaseUrl: NSURL, iCloudUrl: NSURL): Int = memScoped {
    val errorPtr: ObjCObjectVar<NSError?> = alloc()
    val fileManager = NSFileManager.defaultManager
    val stagedUrl = iCloudUrl.URLByDeletingLastPathComponent
        ?.URLByAppendingPathComponent(".feedflow-${NSUUID.UUID().UUIDString}.upload") ?: return 2
    try {
        if (!fileManager.copyItemAtURL(databaseUrl, stagedUrl, errorPtr.ptr)) return 2
        val replaced = if (fileManager.fileExistsAtPath(requireNotNull(iCloudUrl.path))) {
            fileManager.replaceItemAtURL(iCloudUrl, stagedUrl, null, 0u, null, errorPtr.ptr)
        } else {
            fileManager.moveItemAtURL(stagedUrl, iCloudUrl, errorPtr.ptr)
        }
        if (replaced && errorPtr.value == null) 0 else 2
    } finally {
        fileManager.removeItemAtURL(stagedUrl, null)
    }
}

internal fun iCloudDownload(
    iCloudUrl: NSURL,
    tempUrl: NSURL,
    databaseUrl: NSURL,
    databaseName: String,
): Int {
    NSFileManager.defaultManager.removeItemAtURL(
        tempUrl,
        null,
    )

    return memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()

        val copied = NSFileManager.defaultManager.copyItemAtURL(
            srcURL = iCloudUrl,
            toURL = tempUrl,
            error = errorPtr.ptr,
        )

        if (!copied || errorPtr.value != null) {
            val error = errorPtr.value
            return if (error != null && error.domain == NSCocoaErrorDomain && error.code == NSFileReadNoSuchFileError) {
                DOWNLOAD_FILE_NOT_FOUND
            } else {
                3
            }
        }

        val result = replaceDatabase(tempUrl, databaseUrl, databaseName)
        return if (result) {
            0
        } else {
            4
        }
    }
}

private fun replaceDatabase(url: NSURL, databaseUrl: NSURL, databaseName: String): Boolean {
    if (!NSFileManager.defaultManager.fileExistsAtPath(requireNotNull(databaseUrl.path))) {
        return NSFileManager.defaultManager.moveItemAtURL(url, databaseUrl, null)
    }
    // Replace the database
    memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()
        val replaced = NSFileManager.defaultManager.replaceItemAtURL(
            originalItemURL = databaseUrl,
            withItemAtURL = url,
            backupItemName = "$databaseName.old",
            options = NSFileManagerItemReplacementUsingNewMetadataOnly,
            error = errorPtr.ptr,
            resultingItemURL = null,
        )

        return replaced && errorPtr.value == null
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

private const val DOWNLOAD_FILE_NOT_FOUND = 5
