package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.charsets.forName
import io.ktor.utils.io.charsets.isSupported
import kotlinx.io.Buffer

class HtmlRetriever(
    private val logger: Logger,
    private val client: HttpClient,
) {
    suspend fun retrieveHtml(url: String): String? {
        try {
            val response = client.get(url)
            val bytes = response.bodyAsBytes()
            val charset = resolveCharset(
                contentTypeHeader = response.headers[HttpHeaders.ContentType],
                bodyBytes = bytes,
            )
            return decodeBytes(bytes, charset)
        } catch (e: Throwable) {
            logger.d(e) { "Unable to retrieve HTML, skipping" }
            return null
        }
    }

    private fun resolveCharset(contentTypeHeader: String?, bodyBytes: ByteArray): Charset {
        val headerCharset = parseCharsetFromContentType(contentTypeHeader)?.let { charsetFromName(it) }
        if (headerCharset != null) return headerCharset

        val bomCharset = detectBomCharset(bodyBytes)
        if (bomCharset != null) return bomCharset

        val sniffedText = decodeBytes(bodyBytes, Charsets.ISO_8859_1)
        val xmlCharset = parseXmlDeclarationCharset(sniffedText)?.let { charsetFromName(it) }
        if (xmlCharset != null) return xmlCharset

        val metaCharset = parseMetaCharset(sniffedText)?.let { charsetFromName(it) }
        if (metaCharset != null) return metaCharset

        return if (isValidUtf8(bodyBytes)) Charsets.UTF_8 else Charsets.ISO_8859_1
    }

    private fun parseCharsetFromContentType(contentTypeHeader: String?): String? {
        if (contentTypeHeader.isNullOrBlank()) return null
        return runCatching { ContentType.parse(contentTypeHeader).parameter("charset") }.getOrNull()
    }

    private fun parseMetaCharset(html: String): String? {
        val headSnippet = html.take(n = META_SNIFF_LIMIT)
        val directCharset = META_CHARSET_REGEX.find(headSnippet)?.groupValues?.get(1)
        if (!directCharset.isNullOrBlank()) return directCharset
        val httpEquiv = META_HTTP_EQUIV_REGEX.find(headSnippet)?.groupValues?.get(1)
        return httpEquiv?.let { META_HTTP_EQUIV_CHARSET_REGEX.find(it)?.groupValues?.get(1) }
    }

    private fun parseXmlDeclarationCharset(html: String): String? {
        val snippet = html.take(n = XML_DECLARATION_SNIFF_LIMIT)
        return XML_DECLARATION_REGEX.find(snippet)?.groupValues?.get(1)
    }

    private fun charsetFromName(name: String): Charset? {
        val trimmed = name.trim().trimEnd(';')
        if (trimmed.isBlank()) return null
        val lower = trimmed.lowercase()
        if (lower == "utf8") return Charsets.UTF_8
        if (lower == "utf-8") return Charsets.UTF_8
        if (lower == "iso-8859-1" || lower == "latin1" || lower == "latin-1") {
            return Charsets.ISO_8859_1
        }
        if (lower == "windows-1252" || lower == "cp1252") {
            return if (Charsets.isSupported("windows-1252")) {
                Charsets.forName("windows-1252")
            } else {
                Charsets.ISO_8859_1
            }
        }
        if (lower == "windows-1254" || lower == "cp1254" || lower == "iso-8859-9") {
            return if (Charsets.isSupported("windows-1254")) {
                Charsets.forName("windows-1254")
            } else {
                Charsets.ISO_8859_1
            }
        }
        return if (Charsets.isSupported(trimmed)) Charsets.forName(trimmed) else null
    }

    private fun detectBomCharset(bytes: ByteArray): Charset? {
        if (bytes.size >= UTF8_BOM_LENGTH &&
            (bytes[0].toInt() and BYTE_MASK) == UTF8_BOM_FIRST &&
            (bytes[1].toInt() and BYTE_MASK) == UTF8_BOM_SECOND &&
            (bytes[2].toInt() and BYTE_MASK) == UTF8_BOM_THIRD
        ) {
            return Charsets.UTF_8
        }
        return null
    }

    private fun decodeBytes(bytes: ByteArray, charset: Charset): String {
        val buffer = Buffer()
        buffer.write(bytes, 0, bytes.size)
        return charset.newDecoder().decode(buffer)
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var index = 0
        while (index < bytes.size) {
            val leadingByte = bytes[index].toInt() and BYTE_MASK
            val sequenceLength = utf8SequenceLength(leadingByte) ?: return false
            if (sequenceLength > UTF8_SEQUENCE_SINGLE &&
                !hasValidContinuation(bytes, index, sequenceLength - UTF8_SEQUENCE_SINGLE)
            ) {
                return false
            }
            if (!hasValidSequenceBounds(bytes, index, leadingByte, sequenceLength)) return false
            index += sequenceLength
        }
        return true
    }

    private fun hasValidContinuation(bytes: ByteArray, startIndex: Int, count: Int): Boolean {
        if (startIndex + count >= bytes.size) return false
        for (i in 1..count) {
            val value = bytes[startIndex + i].toInt() and BYTE_MASK
            if (value !in UTF8_CONTINUATION_MIN..UTF8_CONTINUATION_MAX) return false
        }
        return true
    }

    private companion object {
        private const val META_SNIFF_LIMIT = 4096
        private const val XML_DECLARATION_SNIFF_LIMIT = 256
        private const val BYTE_MASK = 0xFF
        private const val UTF8_BOM_LENGTH = 3
        private const val UTF8_BOM_FIRST = 0xEF
        private const val UTF8_BOM_SECOND = 0xBB
        private const val UTF8_BOM_THIRD = 0xBF
        private const val UTF8_SEQUENCE_SINGLE = 1
        private const val UTF8_SEQUENCE_DOUBLE = 2
        private const val UTF8_SEQUENCE_TRIPLE = 3
        private const val UTF8_SEQUENCE_QUAD = 4
        private const val UTF8_SINGLE_BYTE_MAX = 0x7F
        private const val UTF8_TWO_BYTE_MIN = 0xC2
        private const val UTF8_TWO_BYTE_MAX = 0xDF
        private const val UTF8_THREE_BYTE_MIN = 0xE0
        private const val UTF8_THREE_BYTE_MAX = 0xEF
        private const val UTF8_THREE_BYTE_SURROGATE = 0xED
        private const val UTF8_FOUR_BYTE_MIN = 0xF0
        private const val UTF8_FOUR_BYTE_MAX = 0xF4
        private const val UTF8_CONTINUATION_MIN = 0x80
        private const val UTF8_CONTINUATION_MAX = 0xBF
        private const val UTF8_E0_SECOND_MIN = 0xA0
        private const val UTF8_ED_SECOND_MAX = 0x9F
        private const val UTF8_F0_SECOND_MIN = 0x90
        private const val UTF8_F4_SECOND_MAX = 0x8F
        private val META_CHARSET_REGEX =
            Regex("<meta[^>]*charset\\s*=\\s*['\\\"]?\\s*([^\\s\"'>/]+)", RegexOption.IGNORE_CASE)
        private val META_HTTP_EQUIV_REGEX =
            Regex(
                "<meta[^>]*http-equiv\\s*=\\s*['\\\"]?content-type['\\\"]?[^>]*content\\s*=\\s*['\\\"]([^\"']+)['\\\"]",
                RegexOption.IGNORE_CASE,
            )
        private val META_HTTP_EQUIV_CHARSET_REGEX =
            Regex("charset\\s*=\\s*([^\\s;]+)", RegexOption.IGNORE_CASE)
        private val XML_DECLARATION_REGEX =
            Regex("<\\?xml[^>]*encoding\\s*=\\s*['\\\"]([^\"']+)['\\\"]", RegexOption.IGNORE_CASE)
    }

    private fun utf8SequenceLength(leadingByte: Int): Int? =
        when {
            leadingByte <= UTF8_SINGLE_BYTE_MAX -> UTF8_SEQUENCE_SINGLE
            leadingByte in UTF8_TWO_BYTE_MIN..UTF8_TWO_BYTE_MAX -> UTF8_SEQUENCE_DOUBLE
            leadingByte in UTF8_THREE_BYTE_MIN..UTF8_THREE_BYTE_MAX -> UTF8_SEQUENCE_TRIPLE
            leadingByte in UTF8_FOUR_BYTE_MIN..UTF8_FOUR_BYTE_MAX -> UTF8_SEQUENCE_QUAD
            else -> null
        }

    private fun hasValidSequenceBounds(
        bytes: ByteArray,
        startIndex: Int,
        leadingByte: Int,
        sequenceLength: Int,
    ): Boolean {
        if (sequenceLength == UTF8_SEQUENCE_TRIPLE) {
            val b1 = bytes[startIndex + 1].toInt() and BYTE_MASK
            if (leadingByte == UTF8_THREE_BYTE_MIN && b1 < UTF8_E0_SECOND_MIN) return false
            if (leadingByte == UTF8_THREE_BYTE_SURROGATE && b1 > UTF8_ED_SECOND_MAX) return false
        }
        if (sequenceLength == UTF8_SEQUENCE_QUAD) {
            val b1 = bytes[startIndex + 1].toInt() and BYTE_MASK
            if (leadingByte == UTF8_FOUR_BYTE_MIN && b1 < UTF8_F0_SECOND_MIN) return false
            if (leadingByte == UTF8_FOUR_BYTE_MAX && b1 > UTF8_F4_SECOND_MAX) return false
        }
        return true
    }
}
