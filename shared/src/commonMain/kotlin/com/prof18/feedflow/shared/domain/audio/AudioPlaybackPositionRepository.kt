package com.prof18.feedflow.shared.domain.audio

import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.russhwolf.settings.Settings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AudioPlaybackPositionRepository(private val settings: Settings) {
    private val lock = Lock()

    fun getPosition(itemId: String): Long = lock.withLock {
        readPositions()[itemId]?.coerceAtLeast(0) ?: 0
    }

    fun savePosition(itemId: String, positionMs: Long, durationMs: Long = 0) = lock.withLock {
        if (itemId.isBlank()) return@withLock
        val positions = readPositions().toMutableMap()
        positions.remove(itemId)
        val completed = durationMs > 0 && positionMs >= durationMs
        if (positionMs > 0 && !completed) {
            positions[itemId] = positionMs
        }
        while (positions.size > MAX_SAVED_EPISODES) {
            positions.remove(positions.keys.first())
        }
        settings.putString(POSITIONS_KEY, Json.encodeToString(positions))
    }

    /**
     * Forgets the item's podcast position and every spoken-article position derived from it. Platforms key
     * reader speech as `speech:<itemId>:<contentKey>`, so a fresh start of the item must clear those too.
     */
    fun clearItemPositions(itemId: String) = lock.withLock {
        if (itemId.isBlank()) return@withLock
        val positions = readPositions()
        val speechPrefix = "$SPEECH_PLAYBACK_PREFIX$itemId:"
        val remaining = positions.filterKeys { it != itemId && !it.startsWith(speechPrefix) }
        if (remaining.size != positions.size) {
            settings.putString(POSITIONS_KEY, Json.encodeToString(remaining))
        }
    }

    private fun readPositions(): Map<String, Long> {
        val stored = settings.getStringOrNull(POSITIONS_KEY) ?: return emptyMap()
        return try {
            Json.decodeFromString<Map<String, Long>>(stored)
        } catch (_: SerializationException) {
            emptyMap()
        }
    }

    private companion object {
        const val POSITIONS_KEY = "audio_playback_positions"
        const val MAX_SAVED_EPISODES = 200
        const val SPEECH_PLAYBACK_PREFIX = "speech:"
    }
}
