package com.prof18.feedflow.desktop.utils

import java.net.URI
import java.net.URISyntaxException

internal fun String.sanitizeUrl(): String {
    return try {
        URI(this).toString()
    } catch (_: URISyntaxException) {
        try {
            val colonIndex = this.indexOf(':')
            if (colonIndex != -1) {
                val scheme = this.substring(0, colonIndex)
                val ssp = this.substring(colonIndex + 1)
                URI(scheme, ssp, null).toASCIIString()
            } else {
                this
            }
        } catch (_: Exception) {
            this
        }
    }
}
