package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.FeedItemContentFileHandlerTestImpl
import com.prof18.feedflow.shared.test.testLogger
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderContentCacheMigrationTest {

    private val settingsRepository = SettingsRepository(MapSettings())
    private val fileHandler = FeedItemContentFileHandlerTestImpl()
    private val migration = ReaderContentCacheMigration(
        settingsRepository = settingsRepository,
        feedItemContentFileHandler = fileHandler,
        logger = testLogger,
    )

    @Test
    fun `clears reader content saved in the previous format`() = runTest {
        fileHandler.saveFeedItemContentToFile("item-1", "<h1>Title</h1><p>Body</p>")

        migration.clearOutdatedContent()

        assertFalse(fileHandler.isContentAvailable("item-1"))
    }

    @Test
    fun `clears reader content only once`() = runTest {
        migration.clearOutdatedContent()
        fileHandler.saveFeedItemContentToFile("item-1", "<p>Body</p>")

        migration.clearOutdatedContent()

        assertTrue(fileHandler.isContentAvailable("item-1"))
    }
}
