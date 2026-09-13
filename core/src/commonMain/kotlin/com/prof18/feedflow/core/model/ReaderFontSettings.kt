package com.prof18.feedflow.core.model

data class ReaderFontSettings(
    val fontSize: Int,
    val lineHeight: Int,
    val bodyFont: ReaderFontFamily = ReaderFontFamily.SYSTEM,
    val headlineFont: ReaderFontFamily = ReaderFontFamily.SYSTEM,
)
