package com.prof18.feedflow.shared.domain.recovery

data class FeedSourceRecoveryUiState(
    val verificationRequired: Boolean,
    val approved: Boolean,
    val profileId: String?,
)
