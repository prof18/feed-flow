package com.prof18.feedflow.core.model

enum class RecoveryMode { VerificationRequired, SessionApproved }

enum class RecoveredFeedOperation { Add, Edit, Refresh }

data class FeedRecoveryRecord(
    val accountEpoch: String,
    val sourceId: String,
    val subscriptionUrl: String,
    val effectiveUrl: String,
    val origin: String,
    val mode: RecoveryMode,
    val profileId: String?,
    val statusCode: Int?,
    val retryAfter: String?,
)

data class FeedRecoveryProfile(
    val id: String,
    val accountEpoch: String,
    val origin: String,
    val userAgent: String,
    val engineVersion: String,
    val readerRetained: Boolean,
    val retired: Boolean,
)
