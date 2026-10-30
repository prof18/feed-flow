package com.prof18.feedflow.shared.domain.feeditem

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.data.SettingsRepository

/**
 * Drops saved reader content written in an older format. Version 1 stored the article with its
 * title and site name baked in; since version 2 the reader adds them when rendering, so old
 * files would show them twice.
 */
internal class ReaderContentCacheMigration(
    private val settingsRepository: SettingsRepository,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val logger: Logger,
) {
    suspend fun clearOutdatedContent() {
        if (settingsRepository.getReaderContentCacheVersion() >= CURRENT_VERSION) return
        logger.d { "Clearing reader content saved in an outdated format" }
        feedItemContentFileHandler.clearAllContent()
        settingsRepository.setReaderContentCacheVersion(CURRENT_VERSION)
    }

    private companion object {
        const val CURRENT_VERSION = 2
    }
}
