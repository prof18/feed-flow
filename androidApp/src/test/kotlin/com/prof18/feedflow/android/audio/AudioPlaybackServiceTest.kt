package com.prof18.feedflow.android.audio

import android.app.Application
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.shared.domain.audio.AudioPlaybackPositionRepository
import com.russhwolf.settings.MapSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [Build.VERSION_CODES.O])
class AudioPlaybackServiceTest {
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        startKoin {
            modules(
                module {
                    single {
                        AndroidAudioPlayer(
                            context,
                            AudioPlaybackPositionRepository(MapSettings()),
                            object : SpeechAudioGenerating {
                                override suspend fun generate(segments: List<String>): File =
                                    error("Speech generation is not used in service tests")
                            },
                        )
                    }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `registers its session before any controller connects so playback can post a notification`() {
        val controller = Robolectric.buildService(AudioPlaybackService::class.java).create()
        try {
            val service = controller.get()
            assertEquals(1, service.sessions.size)
            assertTrue(service.isSessionAdded(service.sessions.single()))
        } finally {
            controller.destroy()
        }
    }
}
