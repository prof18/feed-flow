package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.model.BlockedWordAction
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationStatus
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.test.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BlockedWordOperationTest : KoinTestBase() {

    @Test
    fun `trimmed add publishes writing then applied`() = runTest(testDispatcher) {
        val writeGate = CompletableDeferred<Unit>()
        var writtenWord: String? = null
        val viewModel = viewModel(
            add = {
                writtenWord = it
                writeGate.await()
            },
            reload = { true },
        )

        viewModel.onAddWord("  politics  ")
        runCurrent()
        val writing = viewModel.operationStates.value.getValue("politics")
        assertEquals("politics", writing.word)
        assertEquals(BlockedWordAction.Add, writing.action)
        assertEquals(BlockedWordOperationStatus.Writing, writing.status)

        writeGate.complete(Unit)
        advanceUntilIdle()
        assertEquals("politics", writtenWord)
        assertEquals(BlockedWordOperationStatus.Applied, viewModel.operationStates.value.getValue("politics").status)
    }

    @Test
    fun `blank add is a no op`() = runTest(testDispatcher) {
        var writes = 0
        val viewModel = viewModel(add = { writes++ }, reload = { true })

        viewModel.onAddWord("   \n ")
        runCurrent()

        assertEquals(0, writes)
        assertEquals(emptyMap(), viewModel.operationStates.value)
    }

    @Test
    fun `duplicate same action while writing does not enqueue another write`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        var writes = 0
        val viewModel = viewModel(
            add = {
                writes++
                gate.await()
            },
            reload = { true },
        )

        viewModel.onAddWord("term")
        viewModel.onAddWord(" term ")
        assertEquals(1L, viewModel.operationStates.value.getValue("term").generation)
        runCurrent()
        assertEquals(1, writes)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(BlockedWordOperationStatus.Applied, viewModel.operationStates.value.getValue("term").status)
    }

    @Test
    fun `remove publishes action and completes after reload`() = runTest(testDispatcher) {
        val reloadGate = CompletableDeferred<Boolean>()
        var removedWord: String? = null
        val viewModel = viewModel(
            remove = { removedWord = it },
            reload = { reloadGate.await() },
        )

        viewModel.onRemoveWord("term")
        runCurrent()
        val state = viewModel.operationStates.value.getValue("term")
        assertEquals(BlockedWordAction.Remove, state.action)
        assertEquals(BlockedWordOperationStatus.Refreshing, state.status)
        assertEquals("term", removedWord)

        reloadGate.complete(true)
        advanceUntilIdle()
        assertEquals(BlockedWordOperationStatus.Applied, viewModel.operationStates.value.getValue("term").status)
    }

    @Test
    fun `write failure is visible and retry repeats the same action`() = runTest(testDispatcher) {
        var writes = 0
        val viewModel = viewModel(
            add = { if (++writes == 1) error("write failed") },
            reload = { true },
        )

        viewModel.onAddWord("term")
        advanceUntilIdle()
        val failed = viewModel.operationStates.value.getValue("term")
        assertEquals(BlockedWordAction.Add, failed.action)
        assertEquals(BlockedWordOperationStatus.Failed, failed.status)

        viewModel.retryOperation("term")
        advanceUntilIdle()
        val retried = viewModel.operationStates.value.getValue("term")
        assertEquals(BlockedWordAction.Add, retried.action)
        assertEquals(BlockedWordOperationStatus.Applied, retried.status)
        assertNotEquals(failed.generation, retried.generation)
        assertEquals(2, writes)
    }

    @Test
    fun `reload false reports refresh failure and retry does not repeat mutation`() = runTest(testDispatcher) {
        var writes = 0
        var reloads = 0
        val viewModel = viewModel(
            add = { writes++ },
            reload = { ++reloads > 1 },
        )

        viewModel.onAddWord("term")
        advanceUntilIdle()
        val failedRefresh = viewModel.operationStates.value.getValue("term")
        assertEquals(BlockedWordOperationStatus.RefreshFailed, failedRefresh.status)
        assertEquals(1, writes)

        viewModel.retryOperation("term")
        advanceUntilIdle()
        val retried = viewModel.operationStates.value.getValue("term")
        assertEquals(BlockedWordOperationStatus.Applied, retried.status)
        assertEquals(1, writes)
        assertEquals(2, reloads)
    }

    @Test
    fun `opposite actions serialize and older completion cannot replace newer generation`() = runTest(testDispatcher) {
        val addGate = CompletableDeferred<Unit>()
        val writeOrder = mutableListOf<String>()
        val viewModel = viewModel(
            add = {
                writeOrder += "add:start"
                addGate.await()
                writeOrder += "add:end"
            },
            remove = { writeOrder += "remove" },
            reload = { true },
        )

        viewModel.onAddWord("term")
        runCurrent()
        val firstGeneration = viewModel.operationStates.value.getValue("term").generation
        viewModel.onRemoveWord("term")
        val newer = viewModel.operationStates.value.getValue("term")
        assertEquals(BlockedWordAction.Remove, newer.action)
        assertNotEquals(firstGeneration, newer.generation)

        addGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("add:start", "add:end", "remove"), writeOrder)
        assertEquals(BlockedWordAction.Remove, viewModel.operationStates.value.getValue("term").action)
        assertEquals(BlockedWordOperationStatus.Applied, viewModel.operationStates.value.getValue("term").status)
    }

    @Test
    fun `retry while writing or after applied is a no op`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        var writes = 0
        val viewModel = viewModel(
            add = {
                writes++
                gate.await()
            },
            reload = { true },
        )

        viewModel.onAddWord("term")
        runCurrent()
        viewModel.retryOperation("term")
        runCurrent()
        assertEquals(1, writes)

        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.retryOperation("term")
        advanceUntilIdle()
        assertEquals(1, writes)
        assertEquals(BlockedWordOperationStatus.Applied, viewModel.operationStates.value.getValue("term").status)
    }

    @Test
    fun `cancellation during write is not reported as failure`() = runTest(testDispatcher) {
        val viewModel = viewModel(
            add = { throw CancellationException("cancelled") },
            reload = { error("must not reload") },
        )

        viewModel.onAddWord("term")
        runCurrent()

        assertEquals(BlockedWordOperationStatus.Writing, viewModel.operationStates.value.getValue("term").status)
    }

    private fun viewModel(
        add: suspend (String) -> Unit = {},
        remove: suspend (String) -> Unit = {},
        reload: suspend () -> Boolean,
    ): BlockedWordsViewModel = BlockedWordsViewModel(
        databaseHelper = get<DatabaseHelper>(),
        feedStateRepository = get<FeedStateRepository>(),
        writeAddedWord = add,
        writeRemovedWord = remove,
        reloadFeeds = reload,
    )
}
