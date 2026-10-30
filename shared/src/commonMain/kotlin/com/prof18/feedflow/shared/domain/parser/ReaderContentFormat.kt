package com.prof18.feedflow.shared.domain.parser

import com.prof18.klead.KleadOutput

/** Desktop renders reader content as Markdown; Android and iOS render HTML in a web view. */
internal expect val readerContentFormat: KleadOutput
