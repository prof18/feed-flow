package com.prof18.feedflow.android.audio

import android.app.Application
import android.os.Build
import androidx.media3.common.FlagSet
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.shared.domain.audio.AudioPlaybackPositionRepository
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [Build.VERSION_CODES.O])
class AndroidAudioPlayerTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var audio: AndroidAudioPlayer
    private lateinit var positions: AudioPlaybackPositionRepository
    private lateinit var fake: FakePlayer

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        positions = AudioPlaybackPositionRepository(MapSettings())
        audio = AndroidAudioPlayer(ApplicationProvider.getApplicationContext(), positions)
        fake = FakePlayer()
        audio.attach(fake.player)
    }

    @After
    fun tearDown() {
        audio.detach()
        Dispatchers.resetMain()
    }

    @Test
    fun `restores progress without overwriting it during preparation`() {
        positions.savePosition("a", 12_000)
        audio.playEpisode(episode("a"))
        assertEquals(12_000, fake.position)
        fake.position = 0
        audio.pause()
        assertEquals(12_000, positions.getPosition("a"))
        fake.position = 12_000
        fake.ready()
        assertEquals(12_000, audio.state.value.positionMs)
    }

    @Test
    fun `same episode toggles and replacement saves the preceding identity`() {
        audio.playEpisode(episode("a"))
        fake.ready()
        fake.position = 17_000
        audio.playEpisode(episode("a"))
        assertFalse(fake.playWhenReady)
        assertEquals(17_000, positions.getPosition("a"))
        audio.playEpisode(episode("a"))
        assertTrue(fake.playWhenReady)
        assertEquals(1, fake.replacements)
        positions.savePosition("b", 31_000)
        audio.playEpisode(episode("b"))
        assertEquals("b", audio.state.value.episode?.itemId)
        assertEquals(31_000, fake.position)
        assertEquals(17_000, positions.getPosition("a"))
        audio.pause()
        assertEquals(31_000, positions.getPosition("b"))
    }

    @Test
    fun `rotated enclosure for the same episode reloads the new URL and keeps progress`() {
        val original = episode("a")
        audio.playEpisode(original)
        fake.ready()
        fake.position = 17_000
        val rotated = original.copy(url = "HTTPS://example.com/a.mp3?token=New%2BToken&part=1")
        audio.playEpisode(rotated)
        assertEquals(2, fake.replacements)
        assertEquals(
            "https://example.com/a.mp3?token=New%2BToken&part=1",
            fake.item?.localConfiguration?.uri.toString(),
        )
        assertEquals(17_000, fake.position)
        assertEquals(rotated.url, audio.state.value.episode?.url)
    }

    @Test
    fun `seek close and service destruction preserve progress`() {
        audio.playEpisode(episode("a"))
        fake.ready()
        audio.seek(23_000)
        assertEquals(23_000, positions.getPosition("a"))
        fake.position = 24_000
        audio.close()
        assertNull(audio.state.value.episode)
        assertNull(fake.item)
        assertEquals(24_000, positions.getPosition("a"))
        audio.attach(fake.player)
        audio.playEpisode(episode("a"))
        fake.ready()
        fake.position = 25_000
        audio.detach()
        assertEquals(25_000, positions.getPosition("a"))
        assertNull(audio.state.value.episode)
    }

    @Test
    fun `completion clears progress and replay starts at zero`() {
        audio.playEpisode(episode("a"))
        fake.ready()
        audio.seek(23_000)
        fake.end()
        assertEquals(0, positions.getPosition("a"))
        assertEquals(0, audio.state.value.positionMs)
        assertFalse(audio.state.value.isPlaying)
        audio.togglePlayback()
        assertEquals(0, fake.position)
        assertTrue(fake.playWhenReady)
    }

    @Test
    fun `saves periodically even without player callbacks`() = runTest(dispatcher) {
        audio.playEpisode(episode("a"))
        fake.ready()
        runCurrent()
        fake.position = 21_000
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(21_000, positions.getPosition("a"))
        assertEquals(21_000, audio.state.value.positionMs)
        audio.detach()
    }

    @Test
    fun `failure stops playback saves progress and allows a retry`() {
        audio.playEpisode(episode("a"))
        fake.ready()
        fake.position = 11_000
        fake.fail()
        assertTrue(audio.state.value.failed)
        assertFalse(audio.state.value.isPlaying)
        assertEquals(11_000, positions.getPosition("a"))
        audio.togglePlayback()
        assertFalse(audio.state.value.failed)
        assertTrue(fake.playWhenReady)
        assertEquals(11_000, fake.position)
    }

    @Test
    fun `closing before service creation cancels pending playback`() {
        audio.detach()
        audio.playEpisode(episode("a"))
        audio.close()
        audio.attach(fake.player)
        assertNull(fake.item)
        assertNull(audio.state.value.episode)
    }

    @Test
    fun `pending dock pause prevents autoplay on attachment and resume keeps saved progress`() {
        positions.savePosition("a", 12_000)
        audio.detach()
        audio.playEpisode(episode("a"))
        assertTrue(audio.state.value.isPlaying)
        audio.togglePlayback()
        assertFalse(audio.state.value.isPlaying)
        audio.attach(fake.player)
        assertEquals("a", fake.item?.mediaId)
        assertFalse(fake.playWhenReady)
        assertEquals(12_000, fake.position)
        audio.togglePlayback()
        assertTrue(fake.playWhenReady)
        assertEquals(1, fake.replacements)
        assertEquals(12_000, fake.position)
    }

    @Test
    fun `same episode card cancels pending play before attachment`() {
        audio.detach()
        audio.playEpisode(episode("a"))
        audio.playEpisode(episode("a"))
        assertFalse(audio.state.value.isPlaying)
        audio.attach(fake.player)
        assertFalse(fake.playWhenReady)
        assertEquals(1, fake.replacements)
    }

    @Test
    fun `direct pause before attachment prevents autoplay`() {
        audio.detach()
        audio.playEpisode(episode("a"))
        audio.pause()
        audio.attach(fake.player)
        assertEquals("a", fake.item?.mediaId)
        assertFalse(fake.playWhenReady)
        assertFalse(audio.state.value.isPlaying)
    }

    @Test
    fun `retained service player reattaches for a new episode after close`() {
        audio.playEpisode(episode("a"))
        audio.close()
        positions.savePosition("b", 31_000)
        audio.playEpisode(episode("b"))
        audio.attach(fake.player)
        assertEquals("b", fake.item?.mediaId)
        assertEquals(31_000, fake.position)
        assertTrue(fake.playWhenReady)
        assertEquals(2, fake.replacements)
        audio.attach(fake.player)
        assertEquals(1, fake.listenerCount)
        assertEquals(2, fake.replacements)
    }

    @Test
    fun `attaching the same player twice starts only one ticker`() = runTest(dispatcher) {
        audio.playEpisode(episode("a"))
        fake.ready()
        audio.attach(fake.player)
        runCurrent()
        val readsBeforeTick = fake.positionReads
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, fake.listenerCount)
        assertEquals(1, fake.positionReads - readsBeforeTick)
        audio.detach()
    }

    @Test
    fun `outgoing service destruction cannot detach a replacement player`() {
        audio.playEpisode(episode("a"))
        audio.close()
        val nextPlayer = FakePlayer()
        audio.attach(nextPlayer.player)
        audio.playEpisode(episode("b"))
        audio.detach(fake.player)
        assertEquals("b", audio.state.value.episode?.itemId)
        assertTrue(nextPlayer.playWhenReady)
    }

    @Test
    fun `speed changes while paused do not start playback and resume uses selected speed`() {
        audio.playEpisode(episode("a"))
        fake.ready()
        audio.pause()
        AudioPlaybackSpeed.entries.forEach { speed ->
            audio.setPlaybackSpeed(speed)
            assertEquals(speed.rate, fake.speed)
            assertEquals(speed, audio.state.value.playbackSpeed)
            assertFalse(fake.playWhenReady)
        }
        audio.togglePlayback()
        assertTrue(fake.playWhenReady)
        assertEquals(AudioPlaybackSpeed.DOUBLE.rate, fake.speed)
    }

    @Test
    fun `speed changes while playing preserve progress and survive replacement and close`() {
        audio.playEpisode(episode("a"))
        fake.ready()
        fake.position = 17_000
        audio.setPlaybackSpeed(AudioPlaybackSpeed.ONE_AND_HALF)
        assertTrue(fake.playWhenReady)
        assertEquals(17_000, fake.position)
        assertEquals(AudioPlaybackSpeed.ONE_AND_HALF.rate, fake.speed)
        audio.playEpisode(episode("b"))
        assertEquals(AudioPlaybackSpeed.ONE_AND_HALF, audio.state.value.playbackSpeed)
        audio.close()
        audio.playEpisode(episode("a"))
        val replacement = FakePlayer()
        audio.attach(replacement.player)
        assertEquals(AudioPlaybackSpeed.ONE_AND_HALF.rate, replacement.speed)
        assertEquals(AudioPlaybackSpeed.ONE_AND_HALF, audio.state.value.playbackSpeed)
    }

    @Test
    fun `speed selected before attachment applies without cancelling pending pause`() {
        audio.detach()
        audio.playEpisode(episode("a"))
        audio.pause()
        audio.setPlaybackSpeed(AudioPlaybackSpeed.HALF)
        audio.attach(fake.player)
        assertEquals(AudioPlaybackSpeed.HALF.rate, fake.speed)
        assertFalse(fake.playWhenReady)
    }

    private fun episode(id: String) = AudioEpisode(id, "https://example.com/$id.mp3", id, "Feed", null)

    private class FakePlayer {
        private val listeners = mutableListOf<Player.Listener>()
        var item: MediaItem? = null
        var position = 0L
        var playWhenReady = false
        var speed = 1f
        var playbackState = Player.STATE_IDLE
        var replacements = 0
        val listenerCount get() = listeners.size
        var positionReads = 0
            private set
        private var error: PlaybackException? = null
        val player = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
        ) { _, method, args ->
            when (method.name) {
                "addListener" -> listeners.add(args!![0] as Player.Listener)
                "removeListener" -> listeners.remove(args!![0] as Player.Listener)
                "getCurrentMediaItem" -> item
                "getCurrentPosition" -> {
                    positionReads++
                    position
                }
                "getDuration" -> 90_000L
                "getPlayWhenReady" -> playWhenReady
                "setPlaybackSpeed" -> { speed = args!![0] as Float }
                "getPlaybackState" -> playbackState
                "getPlayerError" -> error
                "isCurrentMediaItemSeekable" -> true
                "setMediaItem" -> {
                    replacements++
                    item = args!![0] as MediaItem
                    position = args[1] as Long
                    emit(Player.EVENT_POSITION_DISCONTINUITY)
                }
                "prepare" -> {
                    error = null
                    playbackState = Player.STATE_BUFFERING
                    emit(Player.EVENT_PLAYBACK_STATE_CHANGED)
                }
                "play", "pause" -> {
                    playWhenReady = method.name == "play"
                    emit(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                }
                "seekTo" -> {
                    position = args!![0] as Long
                    emit(Player.EVENT_POSITION_DISCONTINUITY)
                }
                "stop" -> {
                    playbackState = Player.STATE_IDLE
                    playWhenReady = false
                    emit(Player.EVENT_PLAYBACK_STATE_CHANGED)
                }
                "clearMediaItems" -> item = null
                else -> error("Unexpected Player call: ${method.name}")
            }
        } as Player

        fun ready() {
            playbackState = Player.STATE_READY
            emit(Player.EVENT_PLAYBACK_STATE_CHANGED)
        }

        fun end() {
            playbackState = Player.STATE_ENDED
            position = 90_000
            emit(Player.EVENT_PLAYBACK_STATE_CHANGED)
        }

        fun fail() {
            error = PlaybackException("Failed", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
            listeners.toList().forEach { it.onPlayerError(requireNotNull(error)) }
        }

        private fun emit(event: Int) {
            val events = Player.Events(FlagSet.Builder().add(event).build())
            listeners.toList().forEach { it.onEvents(player, events) }
        }
    }
}
