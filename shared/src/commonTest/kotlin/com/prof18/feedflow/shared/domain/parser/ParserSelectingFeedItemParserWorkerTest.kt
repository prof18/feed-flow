package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame

class ParserSelectingFeedItemParserWorkerTest {

    @Test
    fun `uses legacy parser by default`() = runTest {
        val legacyResult = ParsingResult.Success("legacy", null, null)
        val worker = selector(
            settingsRepository = SettingsRepository(MapSettings()),
            legacyParser = FakeParser(legacyResult),
            kleadParser = FakeParser(ParsingResult.Error),
        )

        assertSame(legacyResult, worker.parse("id", "https://example.com"))
    }

    @Test
    fun `uses Klead parser when enabled and observes later changes`() = runTest {
        val settingsRepository = SettingsRepository(MapSettings())
        val legacyResult = ParsingResult.Success("legacy", null, null)
        val kleadResult = ParsingResult.Success("klead", null, null)
        val worker = selector(
            settingsRepository = settingsRepository,
            legacyParser = FakeParser(legacyResult),
            kleadParser = FakeParser(kleadResult),
        )

        settingsRepository.setKleadParserEnabled(true)
        assertSame(kleadResult, worker.parse("id", "https://example.com"))

        settingsRepository.setKleadParserEnabled(false)
        assertSame(legacyResult, worker.parse("id", "https://example.com"))
    }

    @Test
    fun `engine changes affect the next parse without discarding the current result`() = runTest {
        val settingsRepository = SettingsRepository(MapSettings())
        val legacyResult = ParsingResult.Success("legacy", null, null)
        val kleadResult = ParsingResult.Success("klead", null, null)
        val worker = selector(
            settingsRepository = settingsRepository,
            legacyParser = FakeParser(legacyResult) {
                settingsRepository.setKleadParserEnabled(true)
            },
            kleadParser = FakeParser(kleadResult),
        )

        assertSame(legacyResult, worker.parse("id", "https://example.com"))
        assertSame(kleadResult, worker.parse("id", "https://example.com"))
    }

    private fun selector(
        settingsRepository: SettingsRepository,
        legacyParser: FeedItemParserWorker,
        kleadParser: FeedItemParserWorker,
    ) = ParserSelectingFeedItemParserWorker(
        settingsRepository = settingsRepository,
        legacyParser = legacyParser,
        kleadParser = kleadParser,
    )

    private class FakeParser(
        private val result: ParsingResult,
        private val onParse: suspend () -> Unit = {},
    ) : FeedItemParserWorker {
        override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
            onParse()
            return result
        }
    }
}
