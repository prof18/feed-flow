package com.prof18.ikloud

import com.prof18.feedflow.feedsync.icloud.apple.FoundationICloudFileDiscovery
import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileDiscovery
import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileDiscoveryResult
import com.prof18.jni.JNIEnvVar
import com.prof18.jni.JNI_TRUE
import com.prof18.jni.jboolean
import com.prof18.jni.jclass
import com.prof18.jni.jstring
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileReadNoSuchFileError
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID

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

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_feedsync_ICloudNativeBridge_downloadToFile")
fun downloadToFile(env: CPointer<JNIEnvVar>, clazz: jclass, isDebug: jboolean, destinationPath: jstring?): Int {
    if (destinationPath == null) return 3
    val functions = requireNotNull(env.pointed.value).pointed
    val length = requireNotNull(functions.GetStringLength).invoke(env, destinationPath)
    val chars = requireNotNull(functions.GetStringChars).invoke(env, destinationPath, null) ?: return 3
    val path = try {
        CharArray(length) { chars[it].toInt().toChar() }.concatToString()
    } finally {
        requireNotNull(functions.ReleaseStringChars).invoke(env, destinationPath, chars)
    }
    val iCloudUrl = getICloudFolderURL(isDebug == JNI_TRUE.toUByte()) ?: return 1
    return downloadToFile(iCloudUrl, NSURL.fileURLWithPath(path))
}

internal fun uploadToICloud(
    databaseUrl: NSURL,
    iCloudUrl: NSURL,
    fileCoordinator: ICloudFileCoordinator = FoundationICloudFileCoordinator(),
): Int {
    var result = 2
    val error = fileCoordinator.write(iCloudUrl) { coordinatedUrl ->
        result = replaceCloudFile(databaseUrl, coordinatedUrl)
    }
    return if (error == null) result else 2
}

private fun replaceCloudFile(databaseUrl: NSURL, iCloudUrl: NSURL): Int = memScoped {
    val errorPtr: ObjCObjectVar<NSError?> = alloc()
    errorPtr.value = null
    val fileManager = NSFileManager.defaultManager
    val stagedUrl = iCloudUrl.URLByDeletingLastPathComponent
        ?.URLByAppendingPathComponent(".feedflow-${NSUUID.UUID().UUIDString}.upload") ?: return 2
    try {
        val parentUrl = iCloudUrl.URLByDeletingLastPathComponent ?: return 2
        if (!fileManager.createDirectoryAtURL(parentUrl, true, null, errorPtr.ptr)) return 2
        errorPtr.value = null
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

internal fun downloadToFile(
    iCloudUrl: NSURL,
    destinationUrl: NSURL,
    fileCoordinator: ICloudFileCoordinator = FoundationICloudFileCoordinator(),
    fileDiscovery: ICloudFileDiscovery = FoundationICloudFileDiscovery(),
): Int {
    val discoveredUrl = when (val result = runBlocking { fileDiscovery.discoverAndMaterialize(iCloudUrl) }) {
        is ICloudFileDiscoveryResult.Available -> result.url
        ICloudFileDiscoveryResult.ConfirmedMissing -> return DOWNLOAD_REMOTE_FILE_NOT_FOUND
        is ICloudFileDiscoveryResult.Failure -> return DOWNLOAD_ERROR
    }
    var result = 3
    val error = fileCoordinator.read(discoveredUrl) { coordinatedUrl ->
        result = copyCloudFile(coordinatedUrl, destinationUrl)
    }
    return if (error == null) result else downloadErrorCode(error)
}

private fun copyCloudFile(iCloudUrl: NSURL, destinationUrl: NSURL): Int {
    NSFileManager.defaultManager.removeItemAtURL(destinationUrl, null)
    return memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()
        errorPtr.value = null
        val copied = NSFileManager.defaultManager.copyItemAtURL(iCloudUrl, destinationUrl, errorPtr.ptr)
        if (copied && errorPtr.value == null) {
            0
        } else {
            downloadErrorCode(errorPtr.value)
        }
    }
}

private fun downloadErrorCode(error: NSError?): Int =
    if (error != null && error.domain == NSCocoaErrorDomain && error.code == NSFileReadNoSuchFileError) {
        DOWNLOAD_FILE_NOT_FOUND
    } else {
        3
    }

private fun getDatabaseUrl(isDebug: Boolean): NSURL {
    val path = getDataPath(isDebug)
    val databaseName = getDatabaseName(isDebug)
    val databasePath = "$path/$databaseName.db"

    return NSURL.fileURLWithPath(databasePath)
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
private const val DOWNLOAD_REMOTE_FILE_NOT_FOUND = 6
private const val DOWNLOAD_ERROR = 3
