package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.domain.feeditem.StagedReaderContent
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.testLogger
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class FeedItemContentFileHandlerIosTest {

    private val folder = NSURL.fileURLWithPath("${NSTemporaryDirectory()}feedflow-reader-${NSUUID().UUIDString}")
    private val handler = FeedItemContentFileHandlerIos(TestDispatcherProvider, testLogger) { folder }

    @AfterTest
    fun cleanUp() {
        folder.path?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
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
    fun `promoting creates a first article file`() = runTest {
        val staged = assertNotNull(handler.stageContent("item-1", "First"))

        assertTrue(handler.promoteStaged(staged) { it() })

        assertEquals("First", handler.loadFeedItemContent("item-1"))
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
    }

    @Test
    fun `plain saves still work`() = runTest {
        handler.saveFeedItemContentToFile("item-1", "Saved")

        assertEquals("Saved", handler.loadFeedItemContent("item-1"))
    }

    @Test
    fun `a stage written while stray stages are being deleted survives`() = runTest {
        val fileManager = PausingFileManager()
        val racing = FeedItemContentFileHandlerIos(TestDispatcherProvider, testLogger, fileManager) { folder }
        fileManager.onFirstListing = {
            val otherStage = CoroutineScope(Dispatchers.Default).async { racing.stageContent("item-2", "Second") }
            fileManager.otherStage = otherStage
            runBlocking { withTimeoutOrNull(500.milliseconds) { otherStage.await() } }
        }

        racing.stageContent("item-1", "First")
        val staged = assertNotNull(assertNotNull(fileManager.otherStage).await())

        assertTrue(racing.promoteStaged(staged) { it() })
        assertEquals("Second", racing.loadFeedItemContent("item-2"))
    }

    private fun assertNoStageFiles() {
        val path = requireNotNull(folder.path)
        val files = NSFileManager.defaultManager.contentsOfDirectoryAtPath(path, null).orEmpty()
        assertTrue(files.filterIsInstance<String>().none { it.endsWith(".stage") })
    }
}

/** Pauses the first directory listing, which is the stray-stage cleanup, to let another stage run. */
@OptIn(ExperimentalForeignApi::class)
private class PausingFileManager : NSFileManager() {
    var onFirstListing: (() -> Unit)? = null
    var otherStage: Deferred<StagedReaderContent?>? = null

    override fun contentsOfDirectoryAtPath(path: String, error: CPointer<ObjCObjectVar<NSError?>>?): List<*>? {
        onFirstListing?.also { onFirstListing = null }?.invoke()
        return super.contentsOfDirectoryAtPath(path, error)
    }
}
