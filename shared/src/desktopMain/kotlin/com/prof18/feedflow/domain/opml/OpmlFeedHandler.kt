package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import javax.xml.stream.XMLOutputFactory

internal actual class OpmlFeedHandler(
    private val dispatcherProvider: DispatcherProvider,
) {
    actual suspend fun generateFeedSources(opmlInput: OpmlInput): List<ParsedFeedSource> =
        withContext(dispatcherProvider.io) {
            val feed = opmlInput.file.readText()
            val parser = SAXParserFactory.newInstance().newSAXParser()
            val handler = SaxFeedHandler()
            parser.parse(InputSource(StringReader(feed)), handler)

            return@withContext handler.getFeedSource()
        }

    actual suspend fun exportFeed(
        opmlOutput: OpmlOutput,
        feedSources: List<FeedSource>,
    ) {
        val factory = XMLOutputFactory.newFactory()

        val writer = factory.createXMLStreamWriter(
            BufferedOutputStream(
                FileOutputStream(opmlOutput.file),
            ),
            "UTF-8",
        )

        writer.writeStartDocument("UTF-8", "1.0")
        writer.writeStartElement("opml")
        writer.writeAttribute("version", "1.0")

        writer.writeStartElement("head")
        writer.writeStartElement("title")
        writer.writeCharacters("Subscriptions from FeedFlow")
        writer.writeEndElement()
        writer.writeEndElement()

        writer.writeStartElement("body")

        for (feedSource in feedSources) {
            writer.writeStartElement("outline")
            writer.writeAttribute("type", "rss")
            writer.writeAttribute("text", feedSource.title)
            writer.writeAttribute("title", feedSource.title)
            writer.writeAttribute("xmlUrl", feedSource.url)
            writer.writeAttribute("htmlUrl", feedSource.url)
            writer.writeEndElement()
        }

        writer.writeEndElement()
        writer.writeEndElement()
        writer.writeEndDocument()

        writer.flush()
        writer.close()
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
                OpmlConstants.OUTLINE -> {
                    if (attributes?.getValue(OpmlConstants.XML_URL) == null) {
                        isInsideCategory = true
                        categoryName = attributes?.getValue(OpmlConstants.TITLE)?.trim()
                        if (categoryName == null) {
                            categoryName = attributes?.getValue(OpmlConstants.TEXT)?.trim()
                        }
                    } else {
                        isInsideItem = true
                        parsedFeedBuilder.title(attributes.getValue(OpmlConstants.TITLE)?.trim())
                        parsedFeedBuilder.titleIfNull(attributes.getValue(OpmlConstants.TEXT)?.trim())
                        parsedFeedBuilder.url(attributes.getValue(OpmlConstants.XML_URL)?.trim())
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
