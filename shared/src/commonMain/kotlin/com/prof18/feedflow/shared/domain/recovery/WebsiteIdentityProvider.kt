package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryProfile

interface WebsiteIdentityProvider {
    /** Saved identity for [url] in [profile], or null when the profile is missing, retired or mismatched. */
    suspend fun identity(profile: FeedRecoveryProfile, url: String): WebsiteIdentity?
}

// clearanceCookie is the "cf_clearance=…" pair, or null
data class WebsiteIdentity(val userAgent: String, val clearanceCookie: String?)
