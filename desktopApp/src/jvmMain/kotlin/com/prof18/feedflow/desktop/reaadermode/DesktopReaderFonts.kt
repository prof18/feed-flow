package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.prof18.feedflow.core.model.ReaderFontFamily

// The reader uses these weights (body, Jewel's SemiBold headings, bold, extra bold); each instance
// pins the variable font's weight axis so it renders the real weight instead of a synthetic one.
private val readerFontWeights = listOf(
    FontWeight.Normal,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

internal fun readerComposeFontFamily(family: ReaderFontFamily): FontFamily? {
    if (!family.isBundled) return null
    val faces = listOfNotNull(
        family.fileName?.let { it to FontStyle.Normal },
        family.italicFileName?.let { it to FontStyle.Italic },
    )
    val fonts = faces.flatMap { (fileName, style) ->
        readerFontWeights.map { weight ->
            Font(
                resource = "reader-fonts/$fileName",
                weight = weight,
                style = style,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        }
    }
    return FontFamily(fonts)
}

@Composable
internal fun rememberReaderComposeFontFamily(family: ReaderFontFamily): FontFamily? =
    remember(family) { readerComposeFontFamily(family) }
