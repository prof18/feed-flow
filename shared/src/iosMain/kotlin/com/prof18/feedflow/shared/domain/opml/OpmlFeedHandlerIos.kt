package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.utils.getValueOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToURL
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class OpmlFeedHandlerIos(
    private val dispatcherProvider: DispatcherProvider,
) : OpmlFeedHandler {
    override suspend fun generateFeedSources(opmlInput: OpmlInput): List<ParsedFeedSource> =
        withContext(dispatcherProvider.default) {
            suspendCancellableCoroutine { continuation ->
                val data = opmlInput.opmlData
                val string = NSString.create(data, NSUTF8StringEncoding)?.toString()
                    ?: throw InvalidOpmlImportException(
                        "Failed to parse OPML file: The selected file is not a valid OPML document.",
                    )

                val cleanString = string.replace("\uFEFF", "")
                    .replace("&(?!(?:amp|lt|gt|apos|quot|#[0-9]+);)".toRegex(), "&amp;")
                    .trimStart()

                val cleanData = NSString.create(string = cleanString).dataUsingEncoding(NSUTF8StringEncoding)
                val delegate = NSXMLParserDelegate(
                    onSuccess = { feedSources ->
                        if (continuation.isActive) {
                            continuation.resume(feedSources)
                        }
                    },
                    onError = { exception ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    },
                )
                val parser = NSXMLParser(cleanData ?: data).apply {
                    this.delegate = delegate
                }
                val didParse = parser.parse()
                if (!didParse && continuation.isActive) {
                    continuation.resumeWithException(
                        InvalidOpmlImportException(
                            "Failed to parse OPML file: The selected file is not a valid OPML document.",
                        ),
                    )
                }
            }
        }

    @Suppress("MaximumLineLength")
    override suspend fun exportFeed(
        opmlOutput: OpmlOutput,
        feedSourcesByCategory: Map<FeedSourceCategory?, List<FeedSource>>,
    ) {
        val opmlString = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="1.0">
                <head>
                    <title>Subscriptions from FeedFlow</title>
                </head>
                <body>
                    ${getFeedSourceWithCategoriesXml(feedSourcesByCategory)}
                
                    ${getFeedSourceWithoutCategories(feedSourcesByCategory)}
                </body>
            </opml>
        """.trimIndent()

        NSString.create(string = opmlString.trim())
            .writeToURL(
                url = opmlOutput.url,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
    }

    @Suppress("MaxLineLength")
    private fun getFeedSourceWithoutCategories(feedSourcesByCategory: Map<FeedSourceCategory?, List<FeedSource>>) =
        feedSourcesByCategory.entries.filter { it.key == null }.joinToString("\n") { (_, feedSources) ->
            feedSources.joinToString("\n") { feedSource ->
                """<outline type="rss" text="${feedSource.title.cleanForXml()}" title="${feedSource.title.cleanForXml()}" xmlUrl="${feedSource.url.cleanForXml()}" htmlUrl="${feedSource.url.cleanForXml()}"/>"""
            }
        }

    @Suppress("MaxLineLength")
    private fun getFeedSourceWithCategoriesXml(
        feedSourcesByCategory: Map<FeedSourceCategory?, List<FeedSource>>,
    ): String =
        feedSourcesByCategory.entries.filter { it.key != null && it.value.isNotEmpty() }
            .joinToString("\n") { (category, sources) ->
                val categoryTitle = category?.title?.cleanForXml()
                val outlines = sources.joinToString("\n") { feedSource ->
                    """<outline type="rss" text="${feedSource.title.cleanForXml()}" title="${feedSource.title.cleanForXml()}" xmlUrl="${feedSource.url.cleanForXml()}" htmlUrl="${feedSource.url.cleanForXml()}"/>"""
                }
                """
            <outline text="$categoryTitle" title="$categoryTitle">
                $outlines
            </outline>
            """
            }

    private fun String.cleanForXml(): String =
        this.replace("&", "&amp;")
}

private class NSXMLParserDelegate(
    private val onSuccess: (List<ParsedFeedSource>) -> Unit,
    private val onError: (InvalidOpmlImportException) -> Unit,
) : NSObject(), NSXMLParserDelegateProtocol {

    private var isInsideCategory: Boolean = false
    private var isInsideItem: Boolean = false

    private var categoryName: String? = null
    private var parsedFeedBuilder: ParsedFeedSource.Builder = ParsedFeedSource.Builder()
    private var currentElement: String? = null

    private val feedSource = mutableListOf<ParsedFeedSource>()
    private var completed = false

    override fun parser(
        parser: NSXMLParser,
        didStartElement: String,
        namespaceURI: String?,
        qualifiedName: String?,
        attributes: Map<Any?, *>,
    ) {
        currentElement = didStartElement
        when (currentElement) {
            OpmlConstants.OUTLINE -> {
                val xmlUrl = if (attributes.containsKey(OpmlConstants.XML_URL)) {
                    attributes.getValueOrNull(OpmlConstants.XML_URL)
                } else {
                    null
                }
                if (xmlUrl == null) {
                    isInsideCategory = true
                    categoryName = (attributes.getValueOrNull(OpmlConstants.TITLE) as? String)?.trim()
                    if (categoryName == null) {
                        categoryName = (attributes.getValueOrNull(OpmlConstants.TEXT) as? String)?.trim()
                    }
                } else {
                    isInsideItem = true
                    parsedFeedBuilder.title((attributes.getValueOrNull(OpmlConstants.TITLE) as? String)?.trim())
                    parsedFeedBuilder.titleIfNull((attributes.getValueOrNull(OpmlConstants.TEXT) as? String)?.trim())
                    parsedFeedBuilder.url((attributes.getValueOrNull(OpmlConstants.XML_URL) as? String)?.trim())
                }
            }
        }
    }

    override fun parser(
        parser: NSXMLParser,
        didEndElement: String,
        namespaceURI: String?,
        qualifiedName: String?,
    ) {
        if (isInsideItem) {
            parsedFeedBuilder.category(categoryName)
            parsedFeedBuilder.build()?.let {
                feedSource.add(it)
            }

            parsedFeedBuilder = ParsedFeedSource.Builder()
            isInsideItem = false
        } else if (isInsideCategory) {
            categoryName = null
            isInsideCategory = false
        }
    }

    override fun parserDidEndDocument(parser: NSXMLParser) {
        if (completed) {
            return
        }
        completed = true
        onSuccess(feedSource)
    }

    override fun parser(parser: NSXMLParser, parseErrorOccurred: NSError) {
        if (completed) {
            return
        }
        completed = true
        val description = parseErrorOccurred.localizedDescription()
            .takeIf { it.isNotBlank() }
            ?: "The selected file is not a valid OPML document."
        onError(InvalidOpmlImportException("Failed to parse OPML file: $description"))
    }
}
