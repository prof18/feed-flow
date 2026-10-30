package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChangeFeedCategoryViewModelTest : KoinTestBase() {

    private val viewModel: ChangeFeedCategoryViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val dropboxSettings: DropboxSettings by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val syncWorker = DelayedFeedSyncWorker()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<FeedSyncWorker> { syncWorker }
    }

    @Test
    fun `saveCategory completes while cloud backup is pending`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "News")
        databaseHelper.insertCategories(listOf(category))
        val feedSource = createFeedSource(id = "source-1", title = "Feed One")
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        dropboxSettings.setDropboxData("test-credentials")

        viewModel.loadFeedSource(feedSource)
        advanceUntilIdle()
        viewModel.onCategorySelected(CategoryId(category.id))

        try {
            viewModel.categoryChangedState.test {
                viewModel.saveCategory()
                assertEquals(Unit, awaitItem())
            }

            assertEquals(category.id, databaseHelper.getFeedSource(feedSource.id)?.category?.id)
            assertTrue(settingsRepository.getIsSyncUploadRequired())
            assertEquals(1, syncWorker.queuedUploads)
            assertEquals(0, syncWorker.immediateUploads)
        } finally {
            syncWorker.finishUpload.complete(Unit)
        }
    }

    @Test
    fun `saveCategory completes with unchanged selection while cloud backup is pending`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "News")
        databaseHelper.insertCategories(listOf(category))
        val feedSource = createFeedSource(id = "source-1", title = "Feed One", category = category)
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))
        dropboxSettings.setDropboxData("test-credentials")

        viewModel.loadFeedSource(feedSource)
        advanceUntilIdle()

        try {
            viewModel.categoryChangedState.test {
                viewModel.saveCategory()
                assertEquals(Unit, awaitItem())
            }

            assertEquals(category.id, databaseHelper.getFeedSource(feedSource.id)?.category?.id)
            assertEquals(1, syncWorker.queuedUploads)
            assertEquals(0, syncWorker.immediateUploads)
        } finally {
            syncWorker.finishUpload.complete(Unit)
        }
    }

    @Test
    fun `saveCategory updates feed source and emits state`() = runTest(testDispatcher) {
        val categoryA = FeedSourceCategory(id = "category-a", title = "News")
        val categoryB = FeedSourceCategory(id = "category-b", title = "Tech")
        databaseHelper.insertCategories(listOf(categoryA, categoryB))

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Feed One",
            category = categoryA,
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        viewModel.loadFeedSource(feedSource)
        advanceUntilIdle()

        viewModel.onCategorySelected(CategoryId(categoryB.id))

        viewModel.categoryChangedState.test {
            viewModel.saveCategory()
            assertEquals(Unit, awaitItem())
        }

        advanceUntilIdle()

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertEquals(categoryB.id, updatedFeedSource.category?.id)
    }

    @Test
    fun `moveFeedSourcesToCategory updates feed sources`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "Moved")
        databaseHelper.insertCategories(listOf(category))

        val feedSourceA = createFeedSource(
            id = "source-a",
            title = "Feed A",
        )
        val feedSourceB = createFeedSource(
            id = "source-b",
            title = "Feed B",
        )
        databaseHelper.insertFeedSource(
            listOf(
                feedSourceA.toParsedFeedSource(),
                feedSourceB.toParsedFeedSource(),
            ),
        )

        viewModel.moveFeedSourcesToCategory(listOf(feedSourceA, feedSourceB), category)
        advanceUntilIdle()

        val updatedSources = databaseHelper.getFeedSources().associateBy { it.id }
        assertEquals(category.id, updatedSources.getValue(feedSourceA.id).category?.id)
        assertEquals(category.id, updatedSources.getValue(feedSourceB.id).category?.id)
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSourceGenerator.feedSource(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
    )
}

private class DelayedFeedSyncWorker : FeedSyncWorker {
    val finishUpload = CompletableDeferred<Unit>()
    var queuedUploads = 0
        private set
    var immediateUploads = 0
        private set

    override fun upload() {
        queuedUploads++
    }

    override suspend fun uploadImmediate() {
        immediateUploads++
        finishUpload.await()
    }

    override suspend fun download(isFirstSync: Boolean): SyncResult = SyncResult.Success
    override suspend fun syncFeedSources(): SyncResult = SyncResult.Success
    override suspend fun syncFeedItems(): SyncResult = SyncResult.Success
}
