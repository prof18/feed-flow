package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ParserSelectionCoordinator(
    private val settingsRepository: SettingsRepository,
) {
    private val mutex = Mutex()
    private var generation = 0L

    suspend fun snapshot(expectedKleadSelection: Boolean? = null): ParserSelectionSnapshot = mutex.withLock {
        ParserSelectionSnapshot(
            useKleadParser = expectedKleadSelection ?: settingsRepository.isKleadParserEnabled(),
            generation = generation,
        )
    }

    suspend fun <T> withSelection(
        snapshot: ParserSelectionSnapshot,
        block: suspend () -> T,
    ): T? = mutex.withLock {
        val isCurrent = generation == snapshot.generation &&
            settingsRepository.isKleadParserEnabled() == snapshot.useKleadParser
        if (!isCurrent) return@withLock null
        block()
    }

    suspend fun updateSelection(
        useKleadParser: Boolean,
        clearParserState: suspend () -> Unit,
    ) = mutex.withLock {
        generation++
        settingsRepository.setKleadParserEnabled(useKleadParser)
        clearParserState()
    }
}

internal data class ParserSelectionSnapshot(
    val useKleadParser: Boolean,
    val generation: Long,
)
