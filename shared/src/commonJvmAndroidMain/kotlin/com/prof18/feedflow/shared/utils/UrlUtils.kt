package com.prof18.feedflow.shared.utils

fun isValidUrl(url: String): Boolean {
    if (url.isBlank()) return false

    val trimmed = url.trim()

    // Check if URL is just the protocol without a host
    if (trimmed.matches(Regex("^https?://\\s*$"))) return false

    // Check if there's actual content after the protocol
    val withoutProtocol = trimmed.removePrefix("https://").removePrefix("http://")
    return withoutProtocol.isNotBlank()
}
