package com.prof18.feedflow.shared.ui.readerfont

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.prof18.feedflow.core.model.ReaderFontFamily

@Composable
expect fun rememberComposeFontFamily(family: ReaderFontFamily): FontFamily?
