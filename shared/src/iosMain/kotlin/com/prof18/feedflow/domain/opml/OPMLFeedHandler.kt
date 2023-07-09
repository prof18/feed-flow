package com.prof18.feedflow.domain.opml

import co.touchlab.kermit.Logger
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal actual class OPMLFeedHandler(
    private val dispatcherProvider: DispatcherProvider,
) {
    actual suspend fun importFeed(opmlInput: OPMLInput): List<ParsedFeedSource> =
        withContext(dispatcherProvider.default) {
            suspendCoroutine { continuation ->
                NSXMLParser(opmlInput.opmlData).apply {
                    delegate = NSXMLParserDelegate { continuation.resume(it) }
                }.parse()
            }
        }

    actual suspend fun exportFeed(
        opmlOutput: OPMLOutput,
        feedSources: List<FeedSource>
    ) {
        val opmlString = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="1.0">
                <head>
                    <title>Subscriptions from FeedFlow</title>
                </head>
                <body>
                ${
                feedSources.joinToString("\n") { feedSource ->
                    """<outline type="rss" text="${feedSource.title}" title="${feedSource.title}" xmlUrl="${feedSource.url}" htmlUrl="${feedSource.url}"/>"""
                }}
                </body>
            </opml>
        """.trimIndent()

        val result = (opmlString.trim() as NSString).writeToURL(url = opmlOutput.url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        Logger.d { "File creation result: $result" }
    }
}

private class NSXMLParserDelegate(
    private val onEnd: (List<ParsedFeedSource>) -> Unit
) : NSObject(), NSXMLParserDelegateProtocol {

    private var isInsideCategory: Boolean = false
    private var isInsideItem: Boolean = false

    private var categoryName: String? = null
    private var parsedFeedBuilder: ParsedFeedSource.Builder = ParsedFeedSource.Builder()
    private var currentElement: String? = null

    private val feedSource = mutableListOf<ParsedFeedSource>()

    override fun parser(
        parser: NSXMLParser,
        didStartElement: String,
        namespaceURI: String?,
        qualifiedName: String?,
        attributes: Map<Any?, *>,
    ) {
        currentElement = didStartElement
        when (currentElement) {
            OPMLConstants.OUTLINE -> {
                val rssAttribute = if (attributes.containsKey(OPMLConstants.TYPE)) {
                    attributes.getValue(OPMLConstants.TYPE)
                } else {
                    null
                }
                if (rssAttribute != OPMLConstants.RSS) {
                    isInsideCategory = true
                    categoryName = (attributes.getValue(OPMLConstants.TITLE) as? String)?.trim()
                } else {
                    isInsideItem = true
                    parsedFeedBuilder.title((attributes.getValue(OPMLConstants.TITLE) as? String)?.trim())
                    parsedFeedBuilder.url((attributes.getValue(OPMLConstants.XML_URL) as? String)?.trim())
                }
            }
        }
    }

    override fun parser(
        parser: NSXMLParser,
        didEndElement: String,
        namespaceURI: String?,
        qualifiedName: String?
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
        onEnd(feedSource)
    }
}
