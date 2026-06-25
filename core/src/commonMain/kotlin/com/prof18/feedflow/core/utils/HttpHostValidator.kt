package com.prof18.feedflow.core.utils

object HttpHostValidator {

    fun isSafeForHttpClient(host: String): Boolean {
        if (host.isBlank()) return false
        return host.all { it.isSafeAsciiHostChar() }
    }

    private fun Char.isSafeAsciiHostChar(): Boolean =
        code < ASCII_LIMIT &&
            (isLetterOrDigit() || this == '-' || this == '.' || this == ':' || this == '[' || this == ']')

    private const val ASCII_LIMIT = 128
}
