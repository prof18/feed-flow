package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
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
    fun `rejects result when parser changes during parsing`() = runTest {
        val settingsRepository = SettingsRepository(MapSettings())
        val worker = selector(
            settingsRepository = settingsRepository,
            legacyParser = FakeParser(ParsingResult.Success("legacy", null, null)) {
                settingsRepository.setKleadParserEnabled(true)
            },
            kleadParser = FakeParser(ParsingResult.Error),
        )

        assertIs<ParsingResult.Error>(worker.parse("id", "https://example.com"))
    }

    @Test
    fun `rejects result after parser changes away and back during parsing`() = runTest {
        val settingsRepository = SettingsRepository(MapSettings())
        val coordinator = ParserSelectionCoordinator(settingsRepository)
        val worker = ParserSelectingFeedItemParserWorker(
            legacyParser = FakeParser(ParsingResult.Success("legacy", null, null)) {
                coordinator.updateSelection(useKleadParser = true) {}
                coordinator.updateSelection(useKleadParser = false) {}
            },
            kleadParser = FakeParser(ParsingResult.Error),
            parserSelectionCoordinator = coordinator,
        )

        assertIs<ParsingResult.Error>(worker.parse("id", "https://example.com"))
    }

    private fun selector(
        settingsRepository: SettingsRepository,
        legacyParser: FeedItemParserWorker,
        kleadParser: FeedItemParserWorker,
    ) = ParserSelectingFeedItemParserWorker(
        legacyParser = legacyParser,
        kleadParser = kleadParser,
        parserSelectionCoordinator = ParserSelectionCoordinator(settingsRepository),
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
