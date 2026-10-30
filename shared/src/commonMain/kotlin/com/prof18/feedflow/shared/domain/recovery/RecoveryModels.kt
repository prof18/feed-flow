package com.prof18.feedflow.shared.domain.recovery

import io.ktor.http.Url
import kotlin.jvm.JvmInline

@JvmInline
value class RecoveryOrigin private constructor(val value: String) {
    companion object {
        // https only, no user info, lowercase host, explicit port ("https://example.com:443")
        fun of(url: String): RecoveryOrigin? = runCatching {
            val parsed = Url(url)
            val hasUserInfo = !parsed.user.isNullOrEmpty() || !parsed.password.isNullOrEmpty()
            if (parsed.protocol.name != "https" || hasUserInfo) return null
            if (parsed.host.isBlank()) return null
            RecoveryOrigin("https://${parsed.host.lowercase()}:${parsed.port}")
        }.getOrNull()
    }
}

data class FeedChallenge(
    val requestedUrl: String,
    val effectiveUrl: String,
    val statusCode: Int,
    val retryAfter: String?,
)

class FeedVerificationRequired(val challenge: FeedChallenge) : Exception("Website verification required")
