package com.prof18.feedflow.shared.domain.parser

internal enum class KleadContentFormat {
    HTML,
    MARKDOWN,
}

/** Desktop renders reader content as Markdown; Android and iOS render HTML in a web view. */
internal expect val readerContentFormat: KleadContentFormat
