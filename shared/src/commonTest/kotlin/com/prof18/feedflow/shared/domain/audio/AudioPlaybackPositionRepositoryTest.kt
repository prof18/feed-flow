package com.prof18.feedflow.shared.domain.audio

import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioPlaybackPositionRepositoryTest : KoinTestBase() {
    @Test
    fun `position survives repository recreation and remains scoped to episode`() {
        val settings = MapSettings()
        val repository = AudioPlaybackPositionRepository(settings)
        repository.savePosition("episode-one", 42_000, 120_000)
        repository.savePosition("episode-two", 10_000, 120_000)

        val restored = AudioPlaybackPositionRepository(settings)
        assertEquals(42_000L, restored.getPosition("episode-one"))
        assertEquals(10_000L, restored.getPosition("episode-two"))
        assertEquals(0L, restored.getPosition("missing"))
    }

    @Test
    fun `completion and invalid positions restart from beginning`() {
        val repository = AudioPlaybackPositionRepository(MapSettings())
        repository.savePosition("episode", 42_000, 120_000)
        repository.savePosition("episode", 120_000, 120_000)
        assertEquals(0L, repository.getPosition("episode"))
        repository.savePosition("episode", -100, 120_000)
        assertEquals(0L, repository.getPosition("episode"))
    }

    @Test
    fun `unknown duration keeps resume position and malformed storage recovers`() {
        val settings = MapSettings()
        settings.putString("audio_playback_positions", "broken-json")
        val repository = AudioPlaybackPositionRepository(settings)
        assertEquals(0L, repository.getPosition("episode"))
        repository.savePosition("episode", 42_000)
        assertEquals(42_000L, AudioPlaybackPositionRepository(settings).getPosition("episode"))
    }

    @Test
    fun `storage keeps the most recently saved two hundred episodes`() {
        val settings = MapSettings()
        val repository = AudioPlaybackPositionRepository(settings)
        repeat(200) { repository.savePosition("episode-$it", 1_000) }
        repository.savePosition("episode-0", 2_000)
        repository.savePosition("new-episode", 3_000)
        val restored = AudioPlaybackPositionRepository(settings)
        assertEquals(2_000L, restored.getPosition("episode-0"))
        assertEquals(0L, restored.getPosition("episode-1"))
        assertEquals(3_000L, restored.getPosition("new-episode"))
    }
}
