package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.shared.domain.readerfont.boldResourceFileName
import com.prof18.feedflow.shared.domain.readerfont.resourceFileName

@Composable
internal fun rememberReaderComposeFontFamily(family: ReaderFontFamily): FontFamily? {
    return remember(family) {
        readerComposeFontFamily(family)
    }
}

internal fun readerComposeFontFamily(family: ReaderFontFamily): FontFamily? {
    if (!family.isBundled) return null
    val regularFile = family.resourceFileName() ?: return null
    val regularBytes = loadReaderFontBytes(regularFile) ?: return null
    val fonts = mutableListOf(
        Font(
            identity = family.cssFamilyName,
            data = regularBytes,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
    )
    family.boldResourceFileName()?.let { boldFile ->
        loadReaderFontBytes(boldFile)?.let { boldBytes ->
            fonts += Font(
                identity = "${family.cssFamilyName}-Bold",
                data = boldBytes,
                weight = FontWeight.Bold,
                style = FontStyle.Normal,
            )
        }
    }
    return FontFamily(fonts)
}

private fun loadReaderFontBytes(fileName: String): ByteArray? =
    Thread.currentThread().contextClassLoader
        ?.getResourceAsStream("reader-fonts/$fileName")
        ?.use { it.readBytes() }
        ?: ClassLoader.getSystemClassLoader()
            .getResourceAsStream("reader-fonts/$fileName")
            ?.use { it.readBytes() }
