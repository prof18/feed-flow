package com.prof18.feedflow.shared.domain.readerfont

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

// iosApp/project.yml copies shared/src/commonMain into the app bundle as the `commonMain` folder.
@OptIn(ExperimentalForeignApi::class)
internal actual fun readReaderFontBytes(fileName: String): ByteArray? {
    val resourcePath = NSBundle.mainBundle.resourcePath ?: return null
    val data = NSData.dataWithContentsOfFile("$resourcePath/commonMain/resources/reader-fonts/$fileName")
        ?: return null
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
    return bytes
}
