package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.domain.feeditem.StagedReaderContent
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.testLogger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.FileFilter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    @Test
    fun `promoting staged content replaces the article file`() = runTest {
        handler.saveFeedItemContentToFile("item-1", "Original")
        val staged = assertNotNull(handler.stageContent("item-1", "Replacement"))
        assertEquals("Original", handler.loadFeedItemContent("item-1"))

        assertTrue(handler.promoteStaged(staged) { it() })

        assertEquals("Replacement", handler.loadFeedItemContent("item-1"))
        assertNoStageFiles()
    }

    @Test
    fun `a failing commit keeps the old file and leaves no stage file`() = runTest {
        handler.saveFeedItemContentToFile("item-1", "Original")
        val staged = assertNotNull(handler.stageContent("item-1", "Replacement"))

        assertFalse(handler.promoteStaged(staged) { error("stale") })
        handler.discardStaged(staged)

        assertEquals("Original", handler.loadFeedItemContent("item-1"))
        assertNoStageFiles()
    }

    @Test
    fun `clearing the cache after staging makes promotion stale`() = runTest {
        val staged = assertNotNull(handler.stageContent("item-1", "Replacement"))

        handler.clearAllContent()

        assertFalse(handler.promoteStaged(staged) { it() })
        assertNull(handler.loadFeedItemContent("item-1"))
        assertNoStageFiles()
    }

    @Test
    fun `stray stage files are deleted on first use`() = runTest {
        articlesDirectory.mkdirs()
        File(articlesDirectory, ".feedflow-reader-old.stage").writeText("left over")
        val fresh = JvmFeedItemContentFileHandler(articlesDirectory, TestDispatcherProvider, testLogger)

        fresh.saveFeedItemContentToFile("item-1", "Saved")

        assertNoStageFiles()
    }

    @Test
    fun `a stage written while stray stages are being deleted survives`() = runTest {
        val otherStage = CompletableFuture<StagedReaderContent?>()
        lateinit var racing: JvmFeedItemContentFileHandler
        // Pauses the first stray-stage listing while another thread stages content
        val directory = object : File(articlesDirectory.path) {
            private val paused = AtomicBoolean(false)

            override fun listFiles(filter: FileFilter?): Array<File>? {
                if (paused.compareAndSet(false, true)) {
                    thread { otherStage.complete(runBlocking { racing.stageContent("item-2", "Second") }) }
                    runCatching { otherStage.get(RACE_WAIT_MILLIS, TimeUnit.MILLISECONDS) }
                }
                return super.listFiles(filter)
            }
        }
        racing = JvmFeedItemContentFileHandler(directory, TestDispatcherProvider, testLogger)

        racing.stageContent("item-1", "First")
        val staged = assertNotNull(otherStage.get(STAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS))

        assertTrue(racing.promoteStaged(staged) { it() })
        assertEquals("Second", racing.loadFeedItemContent("item-2"))
    }

    private fun assertNoStageFiles() {
        assertTrue(articlesDirectory.listFiles().orEmpty().none { it.name.endsWith(".stage") })
    }

    private companion object {
        const val RACE_WAIT_MILLIS = 500L
        const val STAGE_TIMEOUT_SECONDS = 5L
    }
}
