package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.TimeSource

internal class DesktopFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val settingsRepository: SettingsRepository,
) : FeedItemParserWorker, ReaderModeParserWarmer {

    private val readerRuntime = ReaderModeJsRuntime(logger)
    private val warmUpScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    private val warmUpStarted = AtomicBoolean(false)

    override fun warmUp() {
        if (!warmUpStarted.compareAndSet(false, true)) return
        warmUpScope.launch {
            try {
                readerRuntime.warmUp()
                logger.d { "GraalJS reader context warmed up" }
            } catch (e: Throwable) {
                warmUpStarted.set(false)
                logger.d(e) { "GraalJS reader warm-up failed" }
            }
        }
    }

    override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
        val totalMark = TimeSource.Monotonic.markNow()
        logger.d { "Reader parse started for: $url (feedItemId: $feedItemId)" }

        return withContext(dispatcherProvider.io) {
            try {
                val dispatchDelayMillis = totalMark.elapsedNow().inWholeMilliseconds
                logger.d { "Reader parser IO dispatch started after ${dispatchDelayMillis}ms for: $url" }

                val retrieveMark = TimeSource.Monotonic.markNow()
                val html = htmlRetriever.retrieveHtml(url)
                val retrieveMillis = retrieveMark.elapsedNow().inWholeMilliseconds
                if (html == null) {
                    logger.d { "Reader HTML retrieval failed after ${retrieveMillis}ms for: $url" }
                    return@withContext ParsingResult.Error
                }
                logger.d {
                    "Reader HTML retrieved in ${retrieveMillis}ms (${html.length} chars) for: $url"
                }
                val cleanupMark = TimeSource.Monotonic.markNow()
                val cleanedHtml = cleanPlaceholderImages(html)
                val preparedHtml = ReaderModeHtmlPreprocessor.prepare(cleanedHtml)
                logger.d {
                    "Reader HTML prepared in ${cleanupMark.elapsedNow().inWholeMilliseconds}ms " +
                        "(input=${html.length} chars, output=${preparedHtml.html.length} chars, " +
                        "removed=${preparedHtml.removedChars} chars, truncated=${preparedHtml.truncated}) for: $url"
                }

                val parseMark = TimeSource.Monotonic.markNow()
                val parseResult = runInterruptible {
                    runReaderParser(preparedHtml.html, url, imageUrl)
                }
                val parseMillis = parseMark.elapsedNow().inWholeMilliseconds
                if (parseResult == null) {
                    logger.d { "Reader parser returned no result after ${parseMillis}ms for: $url" }
                    return@withContext ParsingResult.Error
                }
                logger.d {
                    "Reader parser completed in ${parseMillis}ms \"${parseResult.title}\" " +
                        "(${parseResult.content.length} chars, ${parseResult.timings.toLogString()}) for: $url"
                }

                if (parseResult.content.length < MIN_CONTENT_LENGTH) {
                    logger.d {
                        "Content too short (${parseResult.content.length} chars), rejecting after " +
                            "${totalMark.elapsedNow().inWholeMilliseconds}ms: $url"
                    }
                    return@withContext ParsingResult.Error
                }

                val markdown = buildString {
                    if (!parseResult.title.isNullOrBlank()) {
                        appendLine("# ${parseResult.title}")
                        appendLine()
                    }
                    if (!parseResult.siteName.isNullOrBlank()) {
                        appendLine("**${parseResult.siteName}**")
                        appendLine()
                    }
                    if (!imageUrl.isNullOrBlank()) {
                        appendLine()
                        appendLine("![]($imageUrl)")
                        appendLine()
                    }
                    if (!parseResult.title.isNullOrBlank() || !parseResult.siteName.isNullOrBlank()) {
                        appendLine()
                    }
                    append(parseResult.content)
                }

                val successResult = ParsingResult.Success(
                    htmlContent = markdown,
                    title = parseResult.title,
                    siteName = parseResult.siteName,
                )

                if (settingsRepository.isSaveItemContentOnOpenEnabled()) {
                    val saveMark = TimeSource.Monotonic.markNow()
                    feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, markdown)
                    logger.d { "Reader content cached in ${saveMark.elapsedNow().inWholeMilliseconds}ms for: $url" }
                }

                logger.d {
                    "Reader parse completed in ${totalMark.elapsedNow().inWholeMilliseconds}ms " +
                        "(html=${html.length} chars, content=${parseResult.content.length} chars) for: $url"
                }

                successResult
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.d(e) { "Error parsing content for: $url" }
                ParsingResult.Error
            }
        }
    }

    private fun runReaderParser(html: String, url: String, imageUrl: String?): ReaderModeParseResult? =
        readerRuntime.parse(html, url, imageUrl)

    private fun ReaderModeParseTimings?.toLogString(): String {
        if (this == null) return "js timings unavailable"

        return "js total=${totalMillis ?: "?"}ms, " +
            "dom=${domMillis ?: "?"}ms, " +
            "cleanup=${cleanupMillis ?: "?"}ms, " +
            "defuddle=${defuddleMillis ?: "?"}ms, " +
            "input=${inputChars ?: "?"} chars" +
            defuddleProfiles.toLogString()
    }

    private fun List<ReaderModeDefuddleProfile>.toLogString(): String {
        if (isEmpty()) return ""

        return joinToString(
            separator = "; ",
            prefix = ", defuddle passes=[",
            postfix = "]",
        ) { profile ->
            buildString {
                append(profile.options ?: "unknown")
                append(": total=")
                append(profile.elapsedMillis ?: "?")
                append("ms")
                append(", words=")
                append(profile.wordCount ?: "?")
                append(", chars=")
                append(profile.contentChars ?: "?")
                profile.steps?.takeIf { it.isNotBlank() }?.let { steps ->
                    append(", top=")
                    append(steps)
                }
            }
        }
    }

    private fun cleanPlaceholderImages(html: String): String {
        if (!html.contains(PLACEHOLDER_IMAGE_MARKER)) return html

        return PLACEHOLDER_IMAGE_URL_REGEX.replace(html, "")
    }

    private companion object {
        private const val MIN_CONTENT_LENGTH = 200
        private const val PLACEHOLDER_IMAGE_MARKER = "placeholder.png"

        private val PLACEHOLDER_IMAGE_URL_REGEX = Regex("""https?://[^\s"'<>)]*placeholder\.png""")
    }
}
