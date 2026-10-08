package com.prof18.feedflow.android.audio

import android.app.Application
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowTextToSpeech
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [Build.VERSION_CODES.O])
class AndroidSpeechAudioGeneratorTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generation combines all segments and removes intermediate files`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine()
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            fake.factory(),
        )

        val result = generator.generate(listOf("Title", "Article body"))

        assertTrue(result.isFile)
        assertEquals(listOf("Title", "Article body"), fake.segments)
        assertEquals(48L, result.length())
        assertEquals(4, littleEndianInt(result.readBytes(), 40))
        assertEquals(1, fake.closeCount)
        assertEquals(listOf("speech.wav"), result.parentFile?.list()?.toList())
        result.parentFile?.deleteRecursively()
    }

    @Test
    fun `native initialization uses the injected main dispatcher`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine()
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(
                ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                mainDispatcher = StandardTestDispatcher(testScheduler),
            ),
            fake.factory(),
        )
        val generation = async(start = CoroutineStart.UNDISPATCHED) { generator.generate(listOf("Article")) }

        assertEquals(0, fake.createCount)
        runCurrent()
        val output = generation.await()
        try {
            assertEquals(1, fake.createCount)
            assertEquals(1, fake.closeCount)
            assertTrue(output.isFile)
        } finally {
            output.parentFile?.deleteRecursively()
        }
    }

    private fun testDispatchers(
        ioDispatcher: CoroutineDispatcher,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    ): DispatcherProvider = object : DispatcherProvider {
        override val main = mainDispatcher
        override val io = ioDispatcher
        override val default = ioDispatcher
    }

    @Test
    fun `first generation removes speech orphans and preserves unrelated cache children`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val orphan = File(context.cacheDir, "reader-speech-orphan").apply { mkdirs() }
        val unrelated = File(context.cacheDir, "reader-cache-data").apply { mkdirs() }
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            FakeSpeechEngine().factory(),
        )
        try {
            val output = generator.generate(listOf("Article"))

            assertFalse(orphan.exists())
            assertTrue(unrelated.isDirectory)
            assertTrue(output.isFile)
            output.parentFile?.deleteRecursively()
        } finally {
            unrelated.deleteRecursively()
        }
    }

    @Test
    fun `later generation on same generator preserves prior cached output`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            FakeSpeechEngine().factory(),
        )

        val firstOutput = generator.generate(listOf("First"))
        val secondOutput = generator.generate(listOf("Second"))

        try {
            assertTrue(firstOutput.isFile)
            assertTrue(secondOutput.isFile)
            assertFalse(firstOutput.parentFile == secondOutput.parentFile)
        } finally {
            firstOutput.parentFile?.deleteRecursively()
            secondOutput.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun `empty and overlong UTF16 segments fail before native initialization`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine()
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            fake.factory(),
        )

        expectFailure<IllegalArgumentException> { generator.generate(emptyList()) }
        expectFailure<IllegalArgumentException> { generator.generate(listOf("a".repeat(501))) }
        assertEquals(0, fake.createCount)
    }

    @Test
    fun `synthesis failure closes engine and removes incomplete files`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine().apply { failSynthesis = true }
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            fake.factory(),
        )

        expectFailure<IllegalStateException> { generator.generate(listOf("Article")) }

        assertEquals(1, fake.closeCount)
        assertTrue(speechTempDirectories(context).isEmpty())
    }

    @Test
    fun `native initialization error closes engine and removes owned directory`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine().apply { initializationStatus = android.speech.tts.TextToSpeech.ERROR }
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            fake.factory(),
        )

        expectFailure<IllegalStateException> { generator.generate(listOf("Article")) }

        assertEquals(1, fake.closeCount)
        assertTrue(speechTempDirectories(context).isEmpty())
    }

    @Test
    fun `cancellation closes engine and deletes owned output`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine().apply { blockSynthesis = CompletableDeferred() }
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            fake.factory(),
        )
        val job = async { generator.generate(listOf("Article")) }
        fake.synthesisStarted.await()
        job.cancelAndJoin()

        assertEquals(1, fake.closeCount)
        assertTrue(speechTempDirectories(context).isEmpty())
    }

    @Test
    fun `cancellation after native factory returns closes engine and removes directory`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine()
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            fake.factory(),
        )
        val job = async(start = CoroutineStart.LAZY) { generator.generate(listOf("Article")) }
        fake.cancelJobOnCreate = job

        job.start()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(1, fake.closeCount)
        assertTrue(speechTempDirectories(context).isEmpty())
    }

    @Test
    fun `cancellation at cleanup boundary removes completed but unreturned output`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine()
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = StandardTestDispatcher(testScheduler)),
            fake.factory(),
        )
        val job = async(start = CoroutineStart.LAZY) { generator.generate(listOf("Article")) }
        fake.cancelJobOnClose = job

        job.start()
        runCurrent()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(listOf("Article"), fake.segments)
        assertEquals(1, fake.closeCount)
        assertTrue(speechTempDirectories(context).isEmpty())
    }

    @Test
    fun `native wrapper ignores callback from a different utterance`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val native = TextToSpeech(context) { }
        val shadow = Shadow.extract(native) as ShadowTextToSpeech
        val engine = AndroidSpeechSynthesisEngine(native)
        val destination = File(context.cacheDir, "ignored-speech-callback.wav")
        try {
            val synthesis = async { engine.synthesize("First segment", destination) }
            runCurrent()
            val listener = checkNotNull(shadow.utteranceProgressListener)

            listener.onDone("a-different-request")
            runCurrent()

            assertFalse(synthesis.isCompleted)
            synthesis.cancelAndJoin()
        } finally {
            engine.close()
            destination.delete()
        }
    }

    @Test
    fun `oversized segment fails before later segments are synthesized`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fake = FakeSpeechEngine().apply { oversizedOutput = true }
        val generator = AndroidSpeechAudioGenerator(
            context,
            testDispatchers(ioDispatcher = UnconfinedTestDispatcher(testScheduler)),
            fake.factory(),
        )

        expectFailure<IllegalStateException> { generator.generate(listOf("First", "Second")) }

        assertEquals(listOf("First"), fake.segments)
        assertTrue(speechTempDirectories(context).isEmpty())
    }

    @Test
    fun `WAV with inconsistent PCM format is rejected`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val first = File(context.cacheDir, "source-speech-a.wav")
        val second = File(context.cacheDir, "source-speech-b.wav")
        val output = File(context.cacheDir, "merged-speech-invalid.wav")
        try {
            writePcmWav(first, 16_000, 1, 16, byteArrayOf(1, 2))
            writePcmWav(second, 22_050, 1, 16, byteArrayOf(3, 4))
            expectFailure<IllegalStateException> { WavFileCombiner.combine(listOf(first, second), output) }
            assertFalse(output.exists())
        } finally {
            first.delete()
            second.delete()
            output.delete()
        }
    }

    @Test
    fun `WAV merge rejects input with no PCM frames`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val source = File(context.cacheDir, "source-speech-empty.wav")
        val output = File(context.cacheDir, "merged-speech-empty.wav")
        try {
            writePcmWav(source, 16_000, 1, 16, byteArrayOf())

            expectFailure<IllegalStateException> { WavFileCombiner.combine(listOf(source), output) }

            assertFalse(output.exists())
        } finally {
            source.delete()
            output.delete()
        }
    }

    @Test
    fun `WAV parser skips odd padded metadata chunks`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val source = File(context.cacheDir, "source-speech-odd-chunk.wav")
        val output = File(context.cacheDir, "merged-speech-odd-chunk.wav")
        try {
            writePcmWav(source, 16_000, 1, 16, byteArrayOf(1, 2), oddJunkChunk = true)
            WavFileCombiner.combine(listOf(source), output)
            assertEquals(46L, output.length())
            assertEquals(2, littleEndianInt(output.readBytes(), 40))
        } finally {
            source.delete()
            output.delete()
        }
    }

    @Test
    fun `WAV merge preserves PCM frames and creates seekable standard header`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val source = File(context.cacheDir, "source-speech-test.wav")
        val output = File(context.cacheDir, "merged-speech-test.wav")
        try {
            writePcmWav(source, sampleRate = 16_000, channels = 1, bitsPerSample = 16, data = byteArrayOf(1, 2, 3, 4))

            WavFileCombiner.combine(listOf(source), output)

            assertTrue(output.isFile)
            assertEquals(48L, output.length())
            val bytes = output.readBytes()
            assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
            assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
            assertEquals("data", String(bytes, 36, 4, Charsets.US_ASCII))
            assertEquals(4, littleEndianInt(bytes, 40))
            assertEquals(1, bytes[44].toInt())
            assertEquals(4, bytes[47].toInt())
        } finally {
            source.delete()
            output.delete()
        }
    }

    private fun writePcmWav(
        file: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        data: ByteArray,
        oddJunkChunk: Boolean = false,
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        file.outputStream().use { stream ->
            stream.write("RIFF".toByteArray(Charsets.US_ASCII))
            stream.write(le32(36 + data.size + if (oddJunkChunk) 10 else 0))
            stream.write("WAVE".toByteArray(Charsets.US_ASCII))
            if (oddJunkChunk) {
                stream.write("JUNK".toByteArray(Charsets.US_ASCII))
                stream.write(le32(1))
                stream.write(99)
                stream.write(0)
            }
            stream.write("fmt ".toByteArray(Charsets.US_ASCII))
            stream.write(le32(16))
            stream.write(le16(1))
            stream.write(le16(channels))
            stream.write(le32(sampleRate))
            stream.write(le32(byteRate))
            stream.write(le16(channels * bitsPerSample / 8))
            stream.write(le16(bitsPerSample))
            stream.write("data".toByteArray(Charsets.US_ASCII))
            stream.write(le32(data.size))
            stream.write(data)
        }
    }

    private fun le16(value: Int) = byteArrayOf(value.toByte(), (value ushr 8).toByte())
    private fun le32(value: Int) = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte(),
    )
    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            ((bytes[offset + 3].toInt() and 0xff) shl 24)

    private fun speechTempDirectories(context: Application): List<File> =
        context.cacheDir.listFiles().orEmpty().filter { it.name.startsWith("reader-speech-") }

    private suspend inline fun <reified T : Throwable> expectFailure(crossinline block: suspend () -> Unit) {
        var thrown: Throwable? = null
        try {
            block()
        } catch (failure: Throwable) {
            thrown = failure
        }
        assertTrue("Expected ${T::class.java.simpleName}, got ${thrown?.javaClass?.simpleName}", thrown is T)
    }

    private inner class FakeSpeechEngine {
        val segments = mutableListOf<String>()
        var closeCount = 0
        var createCount = 0
        var failSynthesis = false
        var initializationStatus = android.speech.tts.TextToSpeech.SUCCESS
        var oversizedOutput = false
        var blockSynthesis: CompletableDeferred<Unit>? = null
        var cancelJobOnCreate: Job? = null
        var cancelJobOnClose: Job? = null
        val synthesisStarted = CompletableDeferred<Unit>()

        fun factory() = SpeechSynthesisEngineFactory { _, onInitialized ->
            createCount++
            val engine = object : SpeechSynthesisEngine {
                override suspend fun synthesize(text: String, destination: File) {
                    synthesisStarted.complete(Unit)
                    blockSynthesis?.await()
                    if (failSynthesis) error("fake synthesis failure")
                    segments += text
                    writePcmWav(destination, 16_000, 1, 16, byteArrayOf(1, 2))
                    if (oversizedOutput) {
                        RandomAccessFile(destination, "rw").use { it.setLength(256L * 1024 * 1024 + 1) }
                    }
                }

                override fun close() {
                    closeCount++
                    cancelJobOnClose?.cancel()
                }
            }
            onInitialized(initializationStatus)
            cancelJobOnCreate?.cancel()
            engine
        }
    }
}
