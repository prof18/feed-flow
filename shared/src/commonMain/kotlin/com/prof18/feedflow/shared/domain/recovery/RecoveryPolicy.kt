package com.prof18.feedflow.shared.domain.recovery

object RecoveryPolicy {
    const val CHALLENGE_HEADER = "cf-mitigated"
    const val CHALLENGE_VALUE = "challenge"
    const val REPLAY_COOKIE = "cf_clearance"
    const val PROFILE_ID_PREFIX = "feedflow-recovery-"
    const val MAX_BODY_BYTES = 5 * 1024 * 1024
    const val TRANSFER_CHUNK_BYTES = 64 * 1024
    const val VERIFICATION_TIMEOUT_MILLIS = 120_000L
    const val BROWSER_FETCH_TIMEOUT_MILLIS = 20_000L
    const val SESSION_REQUEST_TIMEOUT_MILLIS = 30_000L

    fun isChallenge(headerValue: String?): Boolean = headerValue?.trim() == CHALLENGE_VALUE
}
