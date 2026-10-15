package com.prof18.feedflow.android.notifications

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil3.request.allowHardware
import coil3.size.Dimension
import coil3.size.Scale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidNotificationImageLoaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `invalid image URLs do no image work`() = runTest {
        val loader = AndroidNotificationImageLoader(context, StandardTestDispatcher(testScheduler)) {
            error("unexpected request")
        }
        for (url in listOf(null, "", "relative.jpg", "file:///tmp/a", "https:///bad", "https://[bad")) {
            assertNull(loader.load(url, 1000))
        }
    }

    @Test
    fun `request disables hardware and asks for aspect fit bounded sampling`() = runTest {
        val bitmap = Bitmap.createBitmap(10, 5, Bitmap.Config.ARGB_8888)
        val loader = AndroidNotificationImageLoader(context, StandardTestDispatcher(testScheduler)) { request ->
            assertFalse(request.allowHardware)
            assertEquals(Scale.FIT, request.scale)
            val size = request.sizeResolver.size()
            assertEquals(Dimension.Pixels(1024), size.width)
            assertEquals(Dimension.Pixels(1024), size.height)
            bitmap
        }
        assertSame(bitmap, loader.load("https://example.com/a.jpg", 1000))
    }

    @Test
    fun `timeout and decode failure fall back to text while cancellation propagates`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val timeoutLoader = AndroidNotificationImageLoader(context, dispatcher) {
            delay(2000)
            null
        }
        assertNull(timeoutLoader.load("https://example.com/a", 1000))
        assertEquals(1000, testScheduler.currentTime)
        val brokenLoader = AndroidNotificationImageLoader(context, dispatcher) { error("decode") }
        assertNull(brokenLoader.load("https://example.com/a", 1000))
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val loader = AndroidNotificationImageLoader(context, dispatcher) {
            try {
                entered.complete(Unit)
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val job = launch { loader.load("https://example.com/a", 1000) }
        entered.await()
        job.cancelAndJoin()
        assertTrue(cancelled.isCompleted)
    }
}
