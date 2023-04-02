package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.dataWithContentsOfURL
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal actual class OPMLFeedParser(
    private val dispatcherProvider: DispatcherProvider,
){
    actual suspend fun parse(opmlInput: OPMLInput): List<ParsedFeedSource> = withContext(dispatcherProvider.default) {
        suspendCoroutine { continuation ->
            NSXMLParser(opmlInput.opmlData).apply {
                delegate = NSXMLParserDelegate { continuation.resume(it) }
            }.parse()
        }
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
