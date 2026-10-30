package com.prof18.feedflow.shared.domain.recovery

/** Deletes the platform website data of retired recovery profiles. */
interface WebsiteProfileCleaner {
    suspend fun clean()
}
