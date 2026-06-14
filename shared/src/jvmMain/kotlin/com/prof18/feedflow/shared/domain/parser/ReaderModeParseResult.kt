package com.prof18.feedflow.shared.domain.parser

internal data class ReaderModeParseResult(
    val content: String,
    val title: String?,
    val siteName: String?,
)
