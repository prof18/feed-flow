package com.prof18.feedflow.android.audio

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.system.measureTimeMillis

private const val MAX_SEGMENT_LENGTH = 500
private const val MAX_OUTPUT_BYTES = 256L * 1024L * 1024L
private const val TAG = "SpeechAudioGenerator"
private const val WAV_RIFF_HEADER_BYTES = 12L
private const val WAV_CHUNK_HEADER_BYTES = 8L
private const val PCM_FORMAT_CHUNK_BYTES = 16
private const val WAV_OUTPUT_HEADER_BYTES = 44L
private const val PCM_FORMAT_CODE = 1
private const val RIFF_FIXED_CHUNK_BYTES = 36L
private const val RIFF_MAX_SIZE = 0xffff_ffffL
private const val RIFF_PAD_FLAG = 1L
private const val UINT8_MASK = 0xffL
private const val UINT8_BITS = 8
private const val UINT32_BYTES = 4

// System TTS generates the segments; one combined file gives Media3 duration, seeking and saved progress.
class AndroidSpeechAudioGenerator internal constructor(
    context: Context,
    private val dispatcherProvider: DispatcherProvider,
    private val engineFactory: SpeechSynthesisEngineFactory = AndroidSpeechSynthesisEngineFactory,
) : SpeechAudioGenerating {
    constructor(context: Context, dispatcherProvider: DispatcherProvider) :
        this(context, dispatcherProvider, AndroidSpeechSynthesisEngineFactory)

    private val appContext = context.applicationContext
    private val cachePreparationMutex = Mutex()
    private var cachePrepared = false

    override suspend fun generate(segments: List<String>): File {
        require(segments.isNotEmpty()) { "At least one speech segment is required" }
        require(segments.all { it.isNotBlank() && it.length <= MAX_SEGMENT_LENGTH }) {
            "Speech segments must be non-empty and at most $MAX_SEGMENT_LENGTH UTF-16 units"
        }

        val ownedDirectory = createOwnedDirectory()
        val output = File(ownedDirectory, "speech.wav")
        var engine: SpeechSynthesisEngine? = null
        var handedOff = false

        try {
            val generatedFile = withTimeout(timeMillis = 5 * 60 * 1_000L) {
                engine = createEngineAndAwaitInit()
                var outputBytes = 0L
                val elapsed = measureTimeMillis {
                    val wavSegments = withContext(dispatcherProvider.io) {
                        segments.mapIndexed { index, segment -> File(ownedDirectory, "segment-$index.wav") to segment }
                    }
                    var generatedBytes = 0L
                    wavSegments.forEach { (file, segment) ->
                        engine!!.synthesize(segment, file)
                        generatedBytes += withContext(dispatcherProvider.io) { file.length() }
                        check(generatedBytes <= MAX_OUTPUT_BYTES) {
                            "Speech audio exceeds the 256 MiB limit"
                        }
                    }
                    withContext(dispatcherProvider.io) {
                        WavFileCombiner.combine(wavSegments.map { it.first }, output)
                        outputBytes = output.length()
                    }
                }
                Log.i(
                    TAG,
                    "Generated speech audio: elapsedMs=$elapsed bytes=$outputBytes segments=${segments.size}",
                )
                output
            }
            withContext(NonCancellable + dispatcherProvider.main) {
                runCatching { engine?.close() }
                engine = null
            }
            withContext(NonCancellable + dispatcherProvider.io) {
                ownedDirectory.listFiles()?.filter { it != output }?.forEach(File::delete)
            }
            handedOff = true
            return generatedFile
        } finally {
            if (!handedOff) {
                withContext(NonCancellable + dispatcherProvider.main) {
                    runCatching { engine?.close() }
                }
                withContext(NonCancellable + dispatcherProvider.io) {
                    ownedDirectory.deleteRecursively()
                }
            }
        }
    }

    private suspend fun createOwnedDirectory(): File {
        var createdDirectory: File? = null
        try {
            val directory = cachePreparationMutex.withLock {
                withContext(NonCancellable + dispatcherProvider.io) {
                    if (!cachePrepared) {
                        val children = checkNotNull(appContext.cacheDir.listFiles()) {
                            "Could not list the application cache directory"
                        }
                        children.filter { it.name.startsWith(SPEECH_DIRECTORY_PREFIX) }.forEach { orphan ->
                            check(orphan.deleteRecursively() || !orphan.exists()) {
                                "Could not remove orphaned speech audio"
                            }
                        }
                        cachePrepared = true
                    }
                    File(appContext.cacheDir, "$SPEECH_DIRECTORY_PREFIX${UUID.randomUUID()}")
                        .also { directory ->
                            check(directory.mkdirs()) { "Could not create temporary speech directory" }
                            createdDirectory = directory
                        }
                }
            }
            currentCoroutineContext().ensureActive()
            return directory
        } catch (failure: Throwable) {
            withContext(NonCancellable + dispatcherProvider.io) { createdDirectory?.deleteRecursively() }
            throw failure
        }
    }

    private suspend fun createEngineAndAwaitInit(): SpeechSynthesisEngine {
        val initialized = CompletableDeferred<Int>()
        var created: SpeechSynthesisEngine? = null
        try {
            withContext(dispatcherProvider.main) {
                // The callback may arrive synchronously, before create() returns its engine.
                created = engineFactory.create(appContext) { status ->
                    if (!initialized.isCompleted) initialized.complete(status)
                }
            }
            val engine = checkNotNull(created) { "Speech engine factory returned no engine" }
            val status = withTimeout(timeMillis = 15_000L) { initialized.await() }
            check(status == TextToSpeech.SUCCESS) { "Speech engine initialization failed: $status" }
            return engine
        } catch (failure: Throwable) {
            withContext(NonCancellable + dispatcherProvider.main) { runCatching { created?.close() } }
            throw failure
        }
    }
}

private const val SPEECH_DIRECTORY_PREFIX = "reader-speech-"

internal fun interface SpeechSynthesisEngineFactory {
    fun create(context: Context, onInitialized: (Int) -> Unit): SpeechSynthesisEngine
}

internal interface SpeechSynthesisEngine {
    suspend fun synthesize(text: String, destination: File)
    fun close()
}

private object AndroidSpeechSynthesisEngineFactory : SpeechSynthesisEngineFactory {
    override fun create(context: Context, onInitialized: (Int) -> Unit): SpeechSynthesisEngine {
        val native = TextToSpeech(context) { status -> onInitialized(status) }
        return AndroidSpeechSynthesisEngine(native)
    }
}

internal class AndroidSpeechSynthesisEngine(
    private val tts: TextToSpeech,
) : SpeechSynthesisEngine {
    private val mainHandler = Handler(Looper.getMainLooper())

    override suspend fun synthesize(text: String, destination: File) {
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val requestID = UUID.randomUUID().toString()
                val resumed = AtomicBoolean(false)
                val listener = object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(callbackID: String?) {
                        if (callbackID == requestID && resumed.compareAndSet(false, true)) {
                            continuation.resume(Unit)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(callbackID: String?) {
                        if (callbackID == requestID) fail(IllegalStateException("Speech synthesis failed"))
                    }

                    override fun onError(callbackID: String?, errorCode: Int) {
                        if (callbackID == requestID) {
                            fail(IllegalStateException("Speech synthesis failed: $errorCode"))
                        }
                    }

                    private fun fail(error: Throwable) {
                        if (resumed.compareAndSet(false, true)) continuation.resumeWithException(error)
                    }
                }
                tts.setOnUtteranceProgressListener(listener)
                continuation.invokeOnCancellation {
                    mainHandler.post { tts.stop() }
                }
                val result = tts.synthesizeToFile(text, Bundle(), destination, requestID)
                if (result != TextToSpeech.SUCCESS && resumed.compareAndSet(false, true)) {
                    continuation.resumeWithException(IllegalStateException("Speech enqueue failed: $result"))
                }
            }
        }
    }

    override fun close() {
        tts.stop()
        tts.shutdown()
    }
}

internal object WavFileCombiner {
    private data class Format(
        val code: Int,
        val channels: Int,
        val sampleRate: Long,
        val byteRate: Long,
        val blockAlign: Int,
        val bitsPerSample: Int,
        val fmtBytes: ByteArray,
    )

    private data class WavInfo(val format: Format, val dataOffset: Long, val dataLength: Long)

    fun combine(inputs: List<File>, output: File) {
        require(inputs.isNotEmpty()) { "No WAV inputs" }
        var reference: Format? = null
        var totalData = 0L
        val wavs = inputs.map { file ->
            val info = inspect(file)
            check(reference == null || sameFormat(reference!!, info.format)) { "Inconsistent speech WAV formats" }
            reference = reference ?: info.format
            totalData = Math.addExact(totalData, info.dataLength)
            check(
                totalData + WAV_OUTPUT_HEADER_BYTES + (totalData and RIFF_PAD_FLAG) <= MAX_OUTPUT_BYTES,
            ) { "Speech audio exceeds the 256 MiB limit" }
            info
        }
        val format = checkNotNull(reference)
        check(format.code == PCM_FORMAT_CODE) { "Only uncompressed PCM speech WAV is supported" }
        check(format.fmtBytes.size == PCM_FORMAT_CHUNK_BYTES) { "Unsupported PCM format chunk" }
        check(totalData > 0L) { "Speech WAV contains no PCM audio" }
        check(format.blockAlign > 0 && totalData % format.blockAlign == 0L) { "Incomplete PCM sample frame" }
        output.parentFile?.mkdirs()
        RandomAccessFile(output, "rw").use { target ->
            target.setLength(0)
            writeHeader(target, format.fmtBytes, totalData)
            inputs.zip(wavs).forEach { (file, info) ->
                RandomAccessFile(file, "r").use { source ->
                    source.seek(info.dataOffset)
                    copyExactly(source, target, info.dataLength)
                }
            }
            if (totalData and RIFF_PAD_FLAG != 0L) target.write(0)
            target.fd.sync()
        }
    }

    private fun inspect(file: File): WavInfo {
        RandomAccessFile(file, "r").use { source ->
            require(
                source.length() >= WAV_RIFF_HEADER_BYTES && readAscii(source, length = 4) == "RIFF",
            ) { "Invalid RIFF WAV" }
            val riffSize = readU32(source)
            require(readAscii(source, length = 4) == "WAVE") { "Invalid WAVE header" }
            val riffEnd = WAV_CHUNK_HEADER_BYTES + riffSize
            require(riffEnd <= source.length() && riffEnd >= WAV_RIFF_HEADER_BYTES) { "Truncated WAV" }
            var format: Format? = null
            var dataOffset: Long? = null
            var dataLength: Long? = null
            while (source.filePointer + WAV_CHUNK_HEADER_BYTES <= riffEnd) {
                val chunkId = readAscii(source, length = 4)
                val chunkLength = readU32(source)
                val chunkStart = source.filePointer
                val nextChunk = Math.addExact(chunkStart, chunkLength + (chunkLength and RIFF_PAD_FLAG))
                require(nextChunk <= riffEnd && nextChunk <= source.length()) { "Truncated WAV chunk" }
                when (chunkId) {
                    "fmt " -> {
                        require(chunkLength == PCM_FORMAT_CHUNK_BYTES.toLong()) { "Unsupported WAV format chunk" }
                        val bytes = ByteArray(PCM_FORMAT_CHUNK_BYTES)
                        source.readFully(bytes)
                        val parsed = Format(
                            code = u16(bytes, offset = 0),
                            channels = u16(bytes, offset = 2),
                            sampleRate = u32(bytes, offset = 4),
                            byteRate = u32(bytes, offset = 8),
                            blockAlign = u16(bytes, offset = 12),
                            bitsPerSample = u16(bytes, offset = 14),
                            fmtBytes = bytes,
                        )
                        require(format == null) { "Duplicate WAV format chunk" }
                        format = parsed
                    }
                    "data" -> {
                        require(dataOffset == null) { "Multiple WAV data chunks are unsupported" }
                        dataOffset = chunkStart
                        dataLength = chunkLength
                    }
                }
                source.seek(nextChunk)
            }
            return WavInfo(
                format = checkNotNull(format) { "WAV format chunk missing" },
                dataOffset = checkNotNull(dataOffset) { "WAV data chunk missing" },
                dataLength = checkNotNull(dataLength),
            )
        }
    }

    private fun sameFormat(left: Format, right: Format): Boolean =
        left.code == right.code && left.channels == right.channels && left.sampleRate == right.sampleRate &&
            left.byteRate == right.byteRate && left.blockAlign == right.blockAlign &&
            left.bitsPerSample == right.bitsPerSample && left.fmtBytes.contentEquals(right.fmtBytes)

    private fun writeHeader(file: RandomAccessFile, fmt: ByteArray, dataLength: Long) {
        val riffLength = RIFF_FIXED_CHUNK_BYTES + dataLength + (dataLength and RIFF_PAD_FLAG)
        require(riffLength <= RIFF_MAX_SIZE) { "WAV exceeds RIFF size limit" }
        file.write("RIFF".toByteArray(Charsets.US_ASCII))
        writeU32(file, riffLength)
        file.write("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        writeU32(file, fmt.size.toLong())
        file.write(fmt)
        file.write("data".toByteArray(Charsets.US_ASCII))
        writeU32(file, dataLength)
    }

    private fun copyExactly(source: RandomAccessFile, target: RandomAccessFile, count: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = count
        while (remaining > 0) {
            val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            check(read > 0) { "Truncated WAV data" }
            target.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun readAscii(file: RandomAccessFile, length: Int): String =
        ByteArray(length).also(file::readFully).toString(Charsets.US_ASCII)

    private fun readU32(file: RandomAccessFile): Long =
        ByteArray(UINT32_BYTES).also(file::readFully).decodeU32(0)

    private fun writeU32(file: RandomAccessFile, value: Long) {
        repeat(UINT32_BYTES) { byteIndex ->
            val shiftBits = byteIndex * UINT8_BITS
            file.write((value ushr shiftBits).toInt() and UINT8_MASK.toInt())
        }
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and UINT8_MASK.toInt()) or
            ((bytes[offset + 1].toInt() and UINT8_MASK.toInt()) shl UINT8_BITS)

    private fun u32(bytes: ByteArray, offset: Int): Long = bytes.decodeU32(offset)

    private fun ByteArray.decodeU32(offset: Int): Long {
        var value = 0L
        repeat(UINT32_BYTES) { byteIndex ->
            value = value or ((this[offset + byteIndex].toLong() and UINT8_MASK) shl (byteIndex * UINT8_BITS))
        }
        return value
    }
}
