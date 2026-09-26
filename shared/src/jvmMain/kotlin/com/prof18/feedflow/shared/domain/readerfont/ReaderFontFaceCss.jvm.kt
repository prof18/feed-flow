package com.prof18.feedflow.shared.domain.readerfont

actual fun readerFontFaceSrc(fileName: String): String? {
    val bytes = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream("reader-fonts/$fileName")
        ?.use { it.readBytes() }
        ?: ClassLoader.getSystemResourceAsStream("reader-fonts/$fileName")
            ?.use { it.readBytes() }
        ?: return null
    return bytes.toFontDataUri()
}
