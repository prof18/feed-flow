package com.prof18.feedflow.core.utils

import io.ktor.http.Url

/**
 * The Darwin Ktor engine feeds the host to `NSURLComponents.percentEncodedHost`, which raises an
 * NSInvalidArgumentException instead of failing the request. Objective-C exceptions are not
 * catchable from Kotlin/Native, so they terminate the app and have to be avoided up front.
 *
 * Ktor percent-encodes the host on its own, but only when it holds a character outside
 * `URLHostAllowedCharacterSet`: '%', ':', '[' and ']' are handed over untouched. Non-ASCII hosts are
 * encoded and resolve to punycode, so they are safe and must stay allowed.
 */
object HttpHostValidator {

    fun isUrlSafeForHttpClient(url: String): Boolean {
        if (url.isBlank()) return false
        val host = runCatching { Url(url.trim()).host }.getOrNull() ?: return false
        return isSafeForHttpClient(host)
    }

    fun isSafeForHttpClient(host: String): Boolean {
        if (host.isBlank()) return false
        if (!host.hasValidPercentEscapes()) return false
        return if (host.startsWith('[')) {
            host.isIpv6Literal()
        } else {
            host.none { it == ':' || it == '[' || it == ']' }
        }
    }

    private fun String.isIpv6Literal(): Boolean =
        endsWith(']') && count { it == '[' } == 1 && count { it == ']' } == 1

    private fun String.hasValidPercentEscapes(): Boolean {
        var index = 0
        while (index < length) {
            if (this[index] != '%') {
                index++
                continue
            }
            if (index + PERCENT_ESCAPE_LENGTH > length) return false
            if (!this[index + 1].isHexDigit() || !this[index + 2].isHexDigit()) return false
            index += PERCENT_ESCAPE_LENGTH
        }
        return true
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private const val PERCENT_ESCAPE_LENGTH = 3
}
