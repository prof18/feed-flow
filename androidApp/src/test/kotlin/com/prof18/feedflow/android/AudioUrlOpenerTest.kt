package com.prof18.feedflow.android

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [Build.VERSION_CODES.O])
class AudioUrlOpenerTest {

    @Test
    fun `successful audio handoff uses an unpinned VIEW intent and original signed url`() {
        val intents = mutableListOf<Intent>()
        val opener = AudioUrlOpener { intents.add(it) }

        assertTrue(opener.open(SIGNED_AUDIO))
        val intent = intents.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(SIGNED_AUDIO, intent.data.toString())
        assertEquals("audio/*", intent.type)
        assertNull(intent.`package`)
        assertNull(intent.component)
        assertFalse(intent.action == Intent.ACTION_CHOOSER)
    }

    @Test
    fun `missing audio handler falls back once to an untyped external VIEW intent`() {
        val intents = mutableListOf<Intent>()
        val opener = AudioUrlOpener {
            intents.add(it)
            if (intents.size == 1) throw ActivityNotFoundException()
        }

        assertTrue(opener.open(SIGNED_AUDIO))
        assertEquals(2, intents.size)
        val fallback = intents.last()
        assertEquals(Intent.ACTION_VIEW, fallback.action)
        assertEquals(SIGNED_AUDIO, fallback.data.toString())
        assertNull(fallback.type)
        assertNull(fallback.`package`)
        assertNull(fallback.component)
    }

    @Test
    fun `both unavailable handlers return false`() {
        val intents = mutableListOf<Intent>()
        val opener = AudioUrlOpener {
            intents.add(it)
            throw ActivityNotFoundException()
        }

        assertFalse(opener.open(SIGNED_AUDIO))
        assertEquals(2, intents.size)
    }

    @Test
    fun `security rejection falls back without crashing`() {
        val intents = mutableListOf<Intent>()
        val opener = AudioUrlOpener {
            intents.add(it)
            if (intents.size == 1) throw SecurityException()
        }

        assertTrue(opener.open(SIGNED_AUDIO))
        assertEquals(2, intents.size)
        assertNull(intents.last().type)
        assertFalse(AudioUrlOpener { throw SecurityException() }.open(SIGNED_AUDIO))
    }

    @Test
    fun `uppercase scheme resolves an audio handler without changing signed path or query`() {
        val handler = IntentFilter(Intent.ACTION_VIEW).apply {
            addDataScheme("https")
            addDataType("audio/*")
        }
        val launched = mutableListOf<Intent>()
        val opener = AudioUrlOpener { intent ->
            if (handler.matchData(intent.type, intent.scheme, intent.data) < 0) {
                throw ActivityNotFoundException()
            }
            launched.add(intent)
        }
        val input = "HTTPS://example.com/EPISODE.mp3?token=A%2Bb&part=1#Track"

        assertTrue(opener.open(input))
        assertEquals(
            "https://example.com/EPISODE.mp3?token=A%2Bb&part=1#Track",
            launched.single().data.toString(),
        )
        assertEquals("audio/*", launched.single().type)
    }

    @Test
    fun `mixed case scheme resolves the external fallback without changing signed payload`() {
        val browser = IntentFilter(Intent.ACTION_VIEW).apply { addDataScheme("https") }
        val attempts = mutableListOf<Intent>()
        val opener = AudioUrlOpener { intent ->
            attempts.add(intent)
            if (intent.type != null || browser.matchData(intent.type, intent.scheme, intent.data) < 0) {
                throw ActivityNotFoundException()
            }
        }
        val input = "hTtPs://example.com/EPISODE.mp3?token=A%2Bb&part=1#Track"

        assertTrue(opener.open(input))
        assertEquals(2, attempts.size)
        assertEquals(
            "https://example.com/EPISODE.mp3?token=A%2Bb&part=1#Track",
            attempts.last().data.toString(),
        )
        assertNull(attempts.last().type)
    }

    @Test
    fun `invalid audio url never launches an intent`() {
        val intents = mutableListOf<Intent>()
        val opener = AudioUrlOpener { intents.add(it) }

        for (url in listOf("", "  ", "https://", "file:///episode.mp3", "episode.mp3")) {
            assertFalse(opener.open(url))
        }
        assertTrue(intents.isEmpty())
    }

    private companion object {
        const val SIGNED_AUDIO = "https://example.com/episode.mp3?token=a%2Bb&part=1#track"
    }
}
