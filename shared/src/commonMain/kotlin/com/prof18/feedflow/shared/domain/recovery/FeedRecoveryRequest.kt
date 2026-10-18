package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory

sealed interface FeedRecoveryRequest {
    val verificationUrl: String
    val retryAfter: String?

    data class Add(
        val subscriptionUrl: String,
        override val verificationUrl: String,
        override val retryAfter: String?,
        val category: FeedSourceCategory?,
        val notificationsEnabled: Boolean,
    ) : FeedRecoveryRequest

    data class Edit(
        val original: FeedSource,
        val replacement: FeedSource,
        override val verificationUrl: String,
        override val retryAfter: String?,
    ) : FeedRecoveryRequest

    data class Refresh(
        val source: FeedSource,
        val record: FeedRecoveryRecord,
    ) : FeedRecoveryRequest {
        override val verificationUrl get() = record.effectiveUrl
        override val retryAfter get() = record.retryAfter
    }
}
