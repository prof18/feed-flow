package com.prof18.feedflow.shared.domain.readerfont

import com.prof18.feedflow.core.model.ReaderFontFamily
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Platform-specific URL usable inside a WebView `@font-face` `src`.
 * All platforms use data URIs so fonts load with an https article baseUrl.
 */
internal expect fun readerFontFaceSrc(fileName: String): String?

private val fontFaceCssCache = mutableMapOf<ReaderFontFamily, String>()

fun readerFontFaceCss(vararg families: ReaderFontFamily): String {
    return families
        .asSequence()
        .filter { it.isBundled }
        .distinct()
        .joinToString("\n") { family ->
            fontFaceCssCache.getOrPut(family) { buildFontFaceCss(family) }
        }
}

private fun buildFontFaceCss(family: ReaderFontFamily): String {
    val regularFile = family.resourceFileName() ?: return ""
    val regularSrc = readerFontFaceSrc(regularFile) ?: return ""
    val boldFile = family.boldResourceFileName()
    val boldSrc = boldFile?.let { readerFontFaceSrc(it) }

    val regularFace = """
        @font-face {
            font-family: '${family.cssFamilyName}';
            src: url('$regularSrc') format('truetype');
            font-weight: 400;
            font-style: normal;
            font-display: swap;
        }
    """.trimIndent()

    if (boldSrc == null) {
        return regularFace
    }

    return regularFace + "\n" + """
        @font-face {
            font-family: '${family.cssFamilyName}';
            src: url('$boldSrc') format('truetype');
            font-weight: 700;
            font-style: normal;
            font-display: swap;
        }
    """.trimIndent()
}

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toFontDataUri(): String =
    "data:font/ttf;base64,${Base64.Default.encode(this)}"
