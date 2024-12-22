package com.prof18.feedflow.shared.utils

internal fun sanitizeUrl(feedUrl: String): String =
    when {
        feedUrl.startsWith("http://") -> feedUrl.trim()
        !feedUrl.startsWith("https://") -> {
            "https://${feedUrl.trim()}"
        }

        else -> feedUrl.trim()
    }
