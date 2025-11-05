package com.prof18.feedflow.shared.domain.opml

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual fun createOpmlInputFromByteArray(data: ByteArray): OpmlInput {
    val nsData = memScoped {
        NSData.create(
            bytes = allocArrayOf(data),
            length = data.size.toULong(),
        )
    }
    return OpmlInput(opmlData = nsData)
}
