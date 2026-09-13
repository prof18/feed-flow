package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.shared.ui.readerfont.loadComposeFontFamily
import com.prof18.feedflow.shared.ui.readerfont.rememberComposeFontFamily

@Composable
internal fun rememberReaderComposeFontFamily(family: ReaderFontFamily): FontFamily? =
    rememberComposeFontFamily(family)

internal fun readerComposeFontFamily(family: ReaderFontFamily): FontFamily? =
    loadComposeFontFamily(family)
