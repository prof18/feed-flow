package com.prof18.feedflow.shared.ui.readerfont

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.prof18.feedflow.core.model.ReaderFontFamily

@Composable
actual fun rememberComposeFontFamily(family: ReaderFontFamily): FontFamily? {
    val context = LocalContext.current.applicationContext
    return remember(family, context) {
        if (!family.isBundled) return@remember null
        val regularFile = family.resourceFileName() ?: return@remember null
        val fonts = mutableListOf(
            Font(
                path = "reader-fonts/$regularFile",
                assetManager = context.assets,
                weight = FontWeight.Normal,
                style = FontStyle.Normal,
            ),
        )
        family.boldResourceFileName()?.let { boldFile ->
            fonts += Font(
                path = "reader-fonts/$boldFile",
                assetManager = context.assets,
                weight = FontWeight.Bold,
                style = FontStyle.Normal,
            )
        }
        FontFamily(fonts)
    }
}
