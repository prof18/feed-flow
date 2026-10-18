package com.prof18.feedflow.shared.domain.recovery

sealed interface ReaderFetchResult {
    data class Content(val content: String, val baseUrl: String, val saveFailed: Boolean = false) : ReaderFetchResult
    data class NeedsVerification(val verificationUrl: String, val blockedUntil: Long?) : ReaderFetchResult
    data object Unavailable : ReaderFetchResult
}
