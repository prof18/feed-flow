package com.prof18.feedflow.shared.domain.readerfont

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun readerFontFaceSrc(fileName: String): String? {
    val path = resolveReaderFontPath(fileName) ?: return null
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
    return bytes.toFontDataUri()
}

private fun resolveReaderFontPath(fileName: String): String? {
    val bundle = NSBundle.mainBundle
    val candidates = listOf(
        Triple("reader-fonts/$fileName", null, null),
        Triple(fileName.removeSuffix(".ttf"), "ttf", "reader-fonts"),
        Triple(fileName.removeSuffix(".ttf"), "ttf", "iosMain/resources/reader-fonts"),
        Triple(fileName.removeSuffix(".ttf"), "ttf", "resources/reader-fonts"),
    )
    for ((name, ext, directory) in candidates) {
        val path = when {
            directory != null && ext != null ->
                bundle.pathForResource(name, ext, directory)
            ext != null ->
                bundle.pathForResource(name, ext)
            else ->
                bundle.pathForResource(name.substringBeforeLast('.'), name.substringAfterLast('.'))
                    ?: bundle.pathForResource(name, null)
        }
        if (path != null) return path
    }
    // Folder-reference copy of iosMain may keep the relative path.
    return bundle.pathForResource("iosMain/resources/reader-fonts/$fileName", null)
        ?: bundle.resourcePath?.let { "$it/iosMain/resources/reader-fonts/$fileName" }
            ?.takeIf { platform.Foundation.NSFileManager.defaultManager.fileExistsAtPath(it) }
        ?: bundle.resourcePath?.let { "$it/resources/reader-fonts/$fileName" }
            ?.takeIf { platform.Foundation.NSFileManager.defaultManager.fileExistsAtPath(it) }
        ?: bundle.resourcePath?.let { "$it/reader-fonts/$fileName" }
            ?.takeIf { platform.Foundation.NSFileManager.defaultManager.fileExistsAtPath(it) }
}
