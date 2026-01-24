package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.test.FakeClock
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.createCsvInput
import com.prof18.feedflow.shared.test.createCsvOutput
import com.prof18.feedflow.shared.test.createFailingCsvOutput
import com.prof18.feedflow.shared.test.createOpmlInput
import com.prof18.feedflow.shared.test.createOpmlOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class ImportExportViewModelTest : KoinTestBase() {

    private val fakeOpmlFeedHandler = FakeOpmlFeedHandler()
    private val viewModel: ImportExportViewModel by inject()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<Clock> { FakeClock.DEFAULT }
        factory<OpmlFeedHandler> { fakeOpmlFeedHandler }
    }

    @Test
    fun `initial state is idle`() = runTest {
        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)
    }

    @Test
    fun `importFeed updates state to loading and success`() = runTest {
        fakeOpmlFeedHandler.feedSources = listOf(SAMPLE_FEED_SOURCE)

        val opmlInput = createOpmlInput(OPML_CONTENT)

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.importFeed(opmlInput)

        runCurrent()
        assertEquals(
            FeedImportExportState.LoadingImport(ImportExportContentType.FeedsOpml),
            viewModel.importExportState.value,
        )
        advanceUntilIdle()
        val success = viewModel.importExportState.value as FeedImportExportState.ImportSuccess
        assertEquals(emptyList(), success.notValidFeedSources.toList())
        assertEquals(emptyList(), success.feedSourceWithError.toList())
    }

    @Test
    fun `exportFeed updates state to loading and success`() = runTest {
        val opmlOutput = createOpmlOutput()

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.exportFeed(opmlOutput)

        runCurrent()
        assertEquals(
            FeedImportExportState.LoadingExport(ImportExportContentType.FeedsOpml),
            viewModel.importExportState.value,
        )
        advanceUntilIdle()
        assertEquals(FeedImportExportState.ExportSuccess, viewModel.importExportState.value)
    }

    @Test
    fun `importArticles updates state to loading and success`() = runTest {
        val csvInput = createCsvInput(CSV_CONTENT)

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.importArticles(csvInput)

        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleImportSuccess, viewModel.importExportState.value)
    }

    @Test
    fun `exportArticles updates state to loading and success`() = runTest {
        val csvOutput = createCsvOutput()

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.exportArticles(
            csvOutput = csvOutput,
            filter = ArticleExportFilter.All,
        )

        advanceUntilIdle()
        assertEquals(FeedImportExportState.ArticleExportSuccess, viewModel.importExportState.value)
    }

    @Test
    fun `importFeed reports error when opml handler fails`() = runTest {
        fakeOpmlFeedHandler.throwOnGenerate = true

        val opmlInput = createOpmlInput(OPML_CONTENT)

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.importFeed(opmlInput)

        runCurrent()
        assertEquals(
            FeedImportExportState.LoadingImport(ImportExportContentType.FeedsOpml),
            viewModel.importExportState.value,
        )
        advanceUntilIdle()
        assertEquals(FeedImportExportState.Error, viewModel.importExportState.value)
    }

    @Test
    fun `exportFeed reports error when opml handler fails`() = runTest {
        fakeOpmlFeedHandler.throwOnExport = true

        val opmlOutput = createOpmlOutput()

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.exportFeed(opmlOutput)

        runCurrent()
        assertEquals(
            FeedImportExportState.LoadingExport(ImportExportContentType.FeedsOpml),
            viewModel.importExportState.value,
        )
        advanceUntilIdle()
        assertEquals(FeedImportExportState.Error, viewModel.importExportState.value)
    }

    @Test
    fun `importArticles reports error when csv is invalid`() = runTest {
        val csvInput = createCsvInput(INVALID_CSV_CONTENT)

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.importArticles(csvInput)

        advanceUntilIdle()
        assertEquals(FeedImportExportState.Error, viewModel.importExportState.value)
    }

    @Test
    fun `exportArticles reports error when output fails`() = runTest {
        val csvOutput = createFailingCsvOutput()

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)

        viewModel.exportArticles(
            csvOutput = csvOutput,
            filter = ArticleExportFilter.All,
        )

        advanceUntilIdle()
        assertEquals(FeedImportExportState.Error, viewModel.importExportState.value)
    }

    @Test
    fun `clearState resets to idle`() = runTest {
        viewModel.startExport(ImportExportContentType.ArticlesCsv)
        viewModel.clearState()

        assertEquals(FeedImportExportState.Idle, viewModel.importExportState.value)
    }

    @Test
    fun `startExport updates loading state`() = runTest {
        viewModel.startExport(ImportExportContentType.FeedsOpml)

        assertEquals(
            FeedImportExportState.LoadingExport(ImportExportContentType.FeedsOpml),
            viewModel.importExportState.value,
        )
    }

    @Test
    fun `reportExportError updates error state`() = runTest {
        viewModel.reportExportError()

        assertEquals(FeedImportExportState.Error, viewModel.importExportState.value)
    }

    @Test
    fun `getCurrentDateForExport uses formatter`() = runTest {
        val dateTime = FakeClock.DEFAULT.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val expected = "${dateTime.day}-${dateTime.month.number}-${dateTime.year}"
        assertEquals(expected, viewModel.getCurrentDateForExport())
    }

    private class FakeOpmlFeedHandler : OpmlFeedHandler {
        var throwOnGenerate: Boolean = false
        var throwOnExport: Boolean = false
        var feedSources: List<ParsedFeedSource> = emptyList()

        override suspend fun generateFeedSources(opmlInput: OpmlInput): List<ParsedFeedSource> {
            simulateWork()
            if (throwOnGenerate) {
                error("Import failed")
            }
            return feedSources
        }

        override suspend fun exportFeed(
            opmlOutput: OpmlOutput,
            feedSourcesByCategory: Map<FeedSourceCategory?, List<FeedSource>>,
        ) {
            simulateWork()
            if (throwOnExport) {
                error("Export failed")
            }
        }

        private suspend fun simulateWork() {
            delay(1)
        }
    }

    private companion object {
        val SAMPLE_FEED_SOURCE = ParsedFeedSource(
            id = "sample-1",
            url = "https://example.com/sample.xml",
            title = "Sample Feed",
            category = null,
            logoUrl = null,
            websiteUrl = null,
        )

        const val OPML_CONTENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="1.0">
                <head>
                    <title>Subscriptions</title>
                </head>
                <body>
                    <outline type="rss" text="Example" title="Example" xmlUrl="https://example.com/feed.xml"/>
                </body>
            </opml>
        """

        const val CSV_CONTENT = "url_hash,url,title,subtitle,image_url,feed_source_id,is_read,is_bookmarked," +
            "pub_date,comments_url,notification_sent,is_blocked\n" +
            "hash-1,https://example.com,Title,Subtitle,https://example.com/image.png,source-1," +
            "false,false,0,,false,false\n"

        const val INVALID_CSV_CONTENT = "url,title\nhttps://example.com,Title\n"
    }
}
