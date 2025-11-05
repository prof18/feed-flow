package com.prof18.feedflow.shared.domain.opml

import java.io.ByteArrayInputStream

actual fun createOpmlInputFromByteArray(data: ByteArray): OpmlInput {
    return OpmlInput(inputStream = ByteArrayInputStream(data))
}
