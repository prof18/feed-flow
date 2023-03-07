package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

actual class OPMLFeedParser(
    private val dispatcherProvider: DispatcherProvider,
) {
    actual suspend fun parse(feed: String): List<ParsedFeedSource> = withContext(dispatcherProvider.io) {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val handler = SaxFeedHandler()
        parser.parse(InputSource(StringReader(feed)), handler)

        return@withContext handler.getFeedSource()
    }

    private class SaxFeedHandler : DefaultHandler() {

        private var isInsideCategory: Boolean = false
        private var isInsideItem: Boolean = false

        private var categoryName: String? = null
        private var parsedFeedBuilder: ParsedFeedSource.Builder = ParsedFeedSource.Builder()

        private val feedSource = mutableListOf<ParsedFeedSource>()

        fun getFeedSource(): List<ParsedFeedSource> = feedSource

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?,
        ) {
            when (qName) {
                OPMLConstants.OUTLINE -> {
                    if (attributes?.getValue(OPMLConstants.TYPE) != OPMLConstants.RSS) {
                        isInsideCategory = true
                        categoryName = attributes?.getValue(OPMLConstants.TITLE)?.trim()
                    } else {
                        isInsideItem = true
                        parsedFeedBuilder.title(attributes.getValue(OPMLConstants.TITLE).trim())
                        parsedFeedBuilder.url(attributes.getValue(OPMLConstants.XML_URL).trim())
                    }
                }
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
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
    }
}
