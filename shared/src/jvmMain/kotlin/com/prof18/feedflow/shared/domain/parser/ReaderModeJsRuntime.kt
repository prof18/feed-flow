package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.time.Duration as JavaDuration

internal class ReaderModeJsRuntime(
    private val logger: Logger,
    private val parseTimeout: Duration = DEFAULT_PARSE_TIMEOUT,
) : AutoCloseable {

    private val engine: Engine by lazy {
        Engine.newBuilder("js")
            .allowExperimentalOptions(true)
            .option("engine.WarnInterpreterOnly", "false")
            .build()
    }

    private val readerSources: List<Source> by lazy {
        listOf(
            jsSource("reader-mode-parser.js"),
        )
    }

    private val contextPool = ArrayBlockingQueue<Context>(MAX_IDLE_CONTEXTS)

    private val parseExecutor = ThreadPoolExecutor(
        0,
        MAX_PARSE_THREADS,
        THREAD_KEEP_ALIVE_SECONDS,
        TimeUnit.SECONDS,
        SynchronousQueue(),
        { runnable ->
            val threadIndex = threadCounter.incrementAndGet()
            Thread(runnable, "graaljs-reader-$threadIndex").apply { isDaemon = true }
        },
        ThreadPoolExecutor.AbortPolicy(),
    )

    fun warmUp() {
        repeat(contextPool.remainingCapacity()) {
            val context = createContext()
            try {
                executeReaderContent(context, WARM_UP_HTML, WARM_UP_URL, null)
                if (!contextPool.offer(context)) {
                    context.close()
                }
            } catch (e: Throwable) {
                closeContextQuietly(context)
                throw e
            }
        }
    }

    fun parse(html: String, url: String, imageUrl: String?): ReaderModeParseResult? {
        val resultJson = executeWithTimeout(html, url, imageUrl) ?: return null
        return try {
            parseResultJson(resultJson)
        } catch (e: Throwable) {
            logger.d(e) { "Unable to decode reader parse result" }
            null
        }
    }

    private fun executeWithTimeout(html: String, url: String, imageUrl: String?): String? {
        val activeContext = AtomicReference<Context?>()
        val timedOut = AtomicBoolean(false)
        val reusable = AtomicBoolean(false)

        val future = try {
            parseExecutor.submit<String> {
                var context: Context? = null
                try {
                    context = acquireContext()
                    activeContext.set(context)
                    val result = executeReaderContent(context, html, url, imageUrl)
                    reusable.set(true)
                    result
                } finally {
                    activeContext.set(null)
                    context?.let {
                        if (reusable.get() && !timedOut.get()) {
                            releaseContext(it)
                        } else {
                            closeContextQuietly(it)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.d(e) { "GraalJS reader executor rejected parse" }
            return null
        }

        return try {
            future.get(parseTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            timedOut.set(true)
            logger.d(e) { "GraalJS reader timed out after $parseTimeout for: $url" }
            cancelActiveParse(activeContext.get(), future)
            null
        } catch (e: InterruptedException) {
            timedOut.set(true)
            cancelActiveParse(activeContext.get(), future)
            Thread.currentThread().interrupt()
            throw CancellationException("GraalJS reader interrupted for: $url").apply {
                initCause(e)
            }
        } catch (e: Throwable) {
            logger.d(e) { "JS error in reader parse" }
            null
        }
    }

    private fun acquireContext(): Context = contextPool.poll() ?: createContext()

    private fun releaseContext(context: Context) {
        if (!contextPool.offer(context)) {
            closeContextQuietly(context)
        }
    }

    private fun createContext(): Context =
        Context.newBuilder("js")
            .engine(engine)
            .allowExperimentalOptions(true)
            .allowAllAccess(false)
            .build()
            .also { context ->
                readerSources.forEach(context::eval)
            }

    private fun executeReaderContent(
        context: Context,
        html: String,
        url: String,
        imageUrl: String?,
    ): String =
        context.getBindings("js")
            .getMember(PARSE_READER_CONTENT_FUNCTION)
            .execute(html, url, imageUrl)
            .asString()

    private fun parseResultJson(resultJson: String): ReaderModeParseResult? {
        val jsObject = Json.parseToJsonElement(resultJson).jsonObject
        jsObject["error"]?.jsonPrimitive?.takeIf { it.isString }?.let {
            logger.d { "Reader parser JS error: ${it.content}" }
            return null
        }

        val content = jsObject["content"]?.jsonPrimitive?.content
        if (content.isNullOrBlank()) {
            logger.d { "Reader parser returned empty content" }
            return null
        }

        val title = jsObject["title"]?.jsonPrimitive?.takeIf { it.isString }?.content
        val siteName = jsObject["siteName"]?.jsonPrimitive?.takeIf { it.isString }?.content
        val timings = jsObject["timings"]?.jsonObject?.let { timingObject ->
            ReaderModeParseTimings(
                totalMillis = timingObject.longValue("totalMillis"),
                domMillis = timingObject.longValue("domMillis"),
                cleanupMillis = timingObject.longValue("cleanupMillis"),
                defuddleMillis = timingObject.longValue("defuddleMillis"),
                inputChars = timingObject.longValue("inputChars"),
                defuddleProfiles = timingObject["defuddleProfiles"]
                    ?.jsonArray
                    ?.mapNotNull { profileElement ->
                        runCatching {
                            profileElement.jsonObject.toDefuddleProfile()
                        }.getOrNull()
                    }
                    .orEmpty(),
            )
        }
        return ReaderModeParseResult(content, title, siteName, timings)
    }

    private fun JsonObject.toDefuddleProfile(): ReaderModeDefuddleProfile =
        ReaderModeDefuddleProfile(
            elapsedMillis = longValue("elapsedMillis"),
            options = stringValue("options"),
            wordCount = longValue("wordCount"),
            contentChars = longValue("contentChars"),
            steps = stringValue("steps"),
        )

    private fun Map<String, JsonElement>.longValue(name: String): Long? =
        this[name]?.jsonPrimitive?.content?.toLongOrNull()

    private fun Map<String, JsonElement>.stringValue(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun cancelActiveParse(context: Context?, future: Future<String>) {
        if (context != null) {
            runCatching {
                context.interrupt(CONTEXT_INTERRUPT_GRACE)
            }.onFailure {
                logger.d(it) { "Unable to interrupt GraalJS reader context" }
            }
            closeContextQuietly(context, cancelIfExecuting = true)
        }
        future.cancel(true)
    }

    private fun closeContextQuietly(
        context: Context,
        cancelIfExecuting: Boolean = false,
    ) {
        runCatching {
            context.close(cancelIfExecuting)
        }.onFailure {
            logger.d(it) { "Unable to close GraalJS reader context" }
        }
    }

    private fun jsSource(name: String): Source =
        Source.newBuilder("js", loadResource(name), name)
            .cached(true)
            .buildLiteral()

    private fun loadResource(name: String): String =
        ReaderModeJsRuntime::class.java
            .getResourceAsStream("/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Could not load $name")

    override fun close() {
        parseExecutor.shutdownNow()
        while (true) {
            val context = contextPool.poll() ?: break
            closeContextQuietly(context)
        }
        engine.close()
    }

    private companion object {
        private const val MAX_IDLE_CONTEXTS = 2
        private const val MAX_PARSE_THREADS = 2
        private const val THREAD_KEEP_ALIVE_SECONDS = 30L
        private const val PARSE_READER_CONTENT_FUNCTION = "parseReaderContent"
        private const val WARM_UP_URL = "https://feedflow.local/reader-warm-up"
        private const val WARM_UP_PARAGRAPH_COUNT = 8

        private val DEFAULT_PARSE_TIMEOUT = 10.seconds
        private val CONTEXT_INTERRUPT_GRACE: JavaDuration = JavaDuration.ofMillis(250)
        private val threadCounter = AtomicInteger()

        private val WARM_UP_HTML = buildString {
            append("<html><head><title>Reader warm up</title></head><body><article>")
            append("<h1>Reader warm up</h1>")
            repeat(WARM_UP_PARAGRAPH_COUNT) { index ->
                append("<p>Warm up paragraph ")
                append(index)
                append(" with enough readable text for the parser to exercise extraction and Markdown output.</p>")
            }
            append("</article></body></html>")
        }
    }
}
