package com.prof18.feedflow.shared.domain.readerfont

internal actual fun readReaderFontBytes(fileName: String): ByteArray? =
    ReaderFontResourceAnchor::class.java.classLoader
        ?.getResourceAsStream("reader-fonts/$fileName")
        ?.use { it.readBytes() }

private object ReaderFontResourceAnchor
