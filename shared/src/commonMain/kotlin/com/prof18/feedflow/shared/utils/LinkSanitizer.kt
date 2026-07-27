package com.prof18.feedflow.shared.utils

internal fun sanitizeUrl(feedUrl: String): String =
    when {
        // URL-less items carry a blank url; keep it blank instead of turning it into "https://".
        feedUrl.isBlank() -> ""
        feedUrl.startsWith("http://") -> feedUrl.trim()
        !feedUrl.startsWith("https://") -> {
            "https://${feedUrl.trim()}"
        }

        else -> feedUrl.trim()
    }
