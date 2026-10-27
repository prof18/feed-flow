package com.prof18.feedflow.android.ui.readermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.prof18.feedflow.core.model.ReaderFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val loadedFamilies = ConcurrentHashMap<ReaderFontFamily, FontFamily>()

/**
 * The bundled reader fonts ship as Java resources of the shared module (the reader page loads them
 * through CSS); Compose needs a file, so each one is copied to the cache once.
 */
@Composable
internal fun rememberReaderFontFamily(family: ReaderFontFamily): FontFamily {
    val cacheDir = LocalContext.current.cacheDir
    val fontFamily by produceState(initialValue = loadedFamilies[family], family) {
        value = loadedFamilies[family] ?: withContext(Dispatchers.IO) {
            loadReaderFontFamily(cacheDir, family)?.also { loadedFamilies[family] = it }
        }
    }
    return fontFamily ?: FontFamily.Default
}

private fun loadReaderFontFamily(cacheDir: File, family: ReaderFontFamily): FontFamily? {
    val fileName = family.fileName ?: return FontFamily.Default
    val fontFile = File(cacheDir, "reader-fonts/$fileName")
    if (!fontFile.exists()) {
        val stream = ReaderFontAnchor::class.java.classLoader
            ?.getResourceAsStream("reader-fonts/$fileName") ?: return null
        fontFile.parentFile?.mkdirs()
        val tempFile = File(fontFile.parentFile, "$fileName.tmp")
        stream.use { input -> tempFile.outputStream().use { input.copyTo(it) } }
        tempFile.renameTo(fontFile)
    }
    // Pinning the variable font's weight axis renders the real weight instead of a synthetic bold.
    return FontFamily(
        listOf(FontWeight.Normal, FontWeight.Bold).map { weight ->
            Font(
                file = fontFile,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )
}

private object ReaderFontAnchor
