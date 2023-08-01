package com.prof18.feedflow.domain.opml

import co.touchlab.kermit.Logger
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.utils.DispatcherProvider
import com.prof18.feedflow.utils.getValueOrNull
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.writeToURL
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal actual class OpmlFeedHandler(
    private val dispatcherProvider: DispatcherProvider,
) {
    actual suspend fun importFeed(opmlInput: OpmlInput): List<ParsedFeedSource> =
        withContext(dispatcherProvider.default) {
            suspendCoroutine { continuation ->
                NSXMLParser(opmlInput.opmlData).apply {
                    delegate = NSXMLParserDelegate { continuation.resume(it) }
                }.parse()
            }
        }

    @Suppress("MaximumLineLength")
    actual suspend fun exportFeed(
        opmlOutput: OpmlOutput,
        feedSources: List<FeedSource>,
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
                """<outline type="rss" text="${feedSource.title.cleanForXml()}" title="${feedSource.title.cleanForXml()}" xmlUrl="${feedSource.url.cleanForXml()}" htmlUrl="${feedSource.url.cleanForXml()}"/>"""
            }}
                </body>
            </opml>
        """.trimIndent()

        (opmlString.trim() as NSString)
            .writeToURL(
                url = opmlOutput.url,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
    }

    private fun String.cleanForXml(): String =
        this.replace("&", "&amp;")
}

private class NSXMLParserDelegate(
    private val onEnd: (List<ParsedFeedSource>) -> Unit,
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
            OpmlConstants.OUTLINE -> {
                val rssAttribute = if (attributes.containsKey(OpmlConstants.TYPE)) {
                    attributes.getValueOrNull(OpmlConstants.TYPE)
                } else {
                    null
                }
                if (rssAttribute != OpmlConstants.RSS) {
                    isInsideCategory = true
                    categoryName = (attributes.getValueOrNull(OpmlConstants.TITLE) as? String)?.trim()
                } else {
                    isInsideItem = true
                    parsedFeedBuilder.title((attributes.getValueOrNull(OpmlConstants.TITLE) as? String)?.trim())
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
        onEnd(feedSource)
    }

    override fun parser(parser: NSXMLParser, parseErrorOccurred: NSError) {
        Logger.d { "ERROR" }
        Logger.d { parseErrorOccurred.localizedDescription() }
    }
}
