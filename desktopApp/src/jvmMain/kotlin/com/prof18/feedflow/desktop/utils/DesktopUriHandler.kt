package com.prof18.feedflow.desktop.utils

import androidx.compose.ui.platform.UriHandler
import java.awt.Desktop
import java.net.URI

private val supportedExternalUriSchemes = setOf("http", "https", "mailto", "magnet")
private const val WEB_AUTHORITY_SEPARATOR = "://"
private const val MIN_PORT = 1
private const val MAX_PORT = 65_535

internal fun UriHandler.openUriSafely(url: String): Boolean {
    val openableUri = url.toOpenableDesktopUri() ?: return false
    return runCatching { openUri(openableUri) }.isSuccess
}

internal fun openDesktopUriSafely(url: String): Boolean {
    val openableUri = url.toOpenableDesktopUri() ?: return false
    return runCatching { Desktop.getDesktop().browse(URI(openableUri)) }.isSuccess
}

internal fun String.toOpenableDesktopUri(): String? {
    val trimmedUrl = trim()
    if (trimmedUrl.isEmpty()) {
        return null
    }

    val schemeCandidate = trimmedUrl.schemeCandidate()
    val normalizedUrl = when {
        schemeCandidate == null -> "https://$trimmedUrl"
        schemeCandidate in supportedExternalUriSchemes -> trimmedUrl
        trimmedUrl.hasSchemeLessHostPort(schemeCandidate) -> "https://$trimmedUrl"
        else -> return null
    }
    val normalizedScheme = normalizedUrl.substringBefore(':').lowercase()
    if ((normalizedScheme == "http" || normalizedScheme == "https") && normalizedUrl.hasInvalidWebAuthority()) {
        return null
    }

    val sanitizedUrl = normalizedUrl.sanitizeUrl().trim()
    val uri = runCatching { URI(sanitizedUrl) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null

    if (scheme !in supportedExternalUriSchemes) {
        return null
    }

    if (scheme == "http" || scheme == "https") {
        if (uri.rawAuthority.isNullOrBlank()) {
            return null
        }
    } else if (uri.rawSchemeSpecificPart.isNullOrBlank()) {
        return null
    }

    return uri.toASCIIString()
}

private fun String.schemeCandidate(): String? {
    val colonIndex = indexOf(':')
    if (colonIndex <= 0) {
        return null
    }

    val candidate = substring(0, colonIndex)
    val isValidSchemeCandidate = candidate.first().isLetter() &&
        candidate.all { it.isLetterOrDigit() || it == '+' || it == '.' || it == '-' }
    return candidate.takeIf { isValidSchemeCandidate }?.lowercase()
}

private fun String.hasSchemeLessHostPort(schemeCandidate: String): Boolean {
    val authorityEndIndex = indexOfAny(charArrayOf('/', '?', '#'))
        .takeIf { it >= 0 }
        ?: length
    val authority = substring(0, authorityEndIndex)
    val port = authority.substringAfter(':', missingDelimiterValue = "")
        .toIntOrNull()
        ?: return false
    val hasSupportedHost = schemeCandidate.contains('.') ||
        schemeCandidate.equals("localhost", ignoreCase = true)
    return hasSupportedHost && port in MIN_PORT..MAX_PORT
}

private fun String.hasInvalidWebAuthority(): Boolean {
    val authorityStartIndex = indexOf(WEB_AUTHORITY_SEPARATOR)
        .takeIf { it >= 0 }
        ?.plus(WEB_AUTHORITY_SEPARATOR.length)
        ?: return true
    val authorityEndIndex = indexOfAny(charArrayOf('/', '?', '#'), startIndex = authorityStartIndex)
        .takeIf { it >= 0 }
        ?: length
    val authority = substring(authorityStartIndex, authorityEndIndex)
    return authority.isBlank() || authority.any(Char::isWhitespace)
}
