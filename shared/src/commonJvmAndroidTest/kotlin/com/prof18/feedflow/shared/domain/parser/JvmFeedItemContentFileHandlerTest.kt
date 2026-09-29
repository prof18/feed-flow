package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.testLogger
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmFeedItemContentFileHandlerTest {

    private val articlesDirectory = File(
        createTempDirectory().toFile(),
        JvmFeedItemContentFileHandler.ARTICLES_DIR,
    )

    private val handler = JvmFeedItemContentFileHandler(
        articlesDirectory = articlesDirectory,
        dispatcherProvider = TestDispatcherProvider,
        logger = testLogger,
    )

    @Test
    fun `saved content can be loaded back`() = runTest {
        handler.saveFeedItemContentToFile("item-1", "Content")

        assertEquals("Content", handler.loadFeedItemContent("item-1"))
    }

    @Test
    fun `content is available after save and gone after delete`() = runTest {
        handler.saveFeedItemContentToFile("item-1", "Content")
        assertTrue(handler.isContentAvailable("item-1"))

        handler.deleteFeedItemContent("item-1")

        assertFalse(handler.isContentAvailable("item-1"))
    }

    @Test
    fun `clearing all content leaves the directory empty`() = runTest {
        handler.saveFeedItemContentToFile("item-1", "One")
        handler.saveFeedItemContentToFile("item-2", "Two")

        handler.clearAllContent()

        assertTrue(articlesDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `loading a missing item returns null`() = runTest {
        assertNull(handler.loadFeedItemContent("missing"))
    }
}
