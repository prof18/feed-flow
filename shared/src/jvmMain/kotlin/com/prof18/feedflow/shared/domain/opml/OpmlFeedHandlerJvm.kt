package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.nio.charset.Charset
import javax.xml.parsers.SAXParserFactory
import javax.xml.stream.XMLOutputFactory

class OpmlParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal class OpmlFeedHandlerJvm(
    private val dispatcherProvider: DispatcherProvider,
) : OpmlFeedHandler {
    override suspend fun generateFeedSources(opmlInput: OpmlInput): List<ParsedFeedSource> =
        withContext(dispatcherProvider.io) {
            val feedBytes = opmlInput.file.readBytes()
            val feed = String(feedBytes, detectCharset(feedBytes))

            try {
                // Remove BOM and leading whitespace
                val cleanFeed = feed.replace("\uFEFF", "").trimStart()

                // Remove any existing XML declaration
                val xmlContent = cleanFeed.replace(Regex("<\\?xml.*?\\?>"), "")

                // Add a proper XML declaration
                val sanitizedFeed = """<?xml version="1.0" encoding="UTF-8"?>
                    |$xmlContent
                """.trimMargin()
                    .replace("&(?!(?:amp|lt|gt|apos|quot|#[0-9]+);)".toRegex(), "&amp;") // Fix unescaped &
                    .replace("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]".toRegex(), "") // Remove control chars

                val factory = SAXParserFactory.newInstance()
                factory.isNamespaceAware = true
                factory.isValidating = false
                val parser = factory.newSAXParser()
                val handler = SaxFeedHandler()
                parser.parse(InputSource(StringReader(sanitizedFeed)), handler)
                return@withContext handler.getFeedSource()
            } catch (e: Exception) {
                throw OpmlParsingException("Failed to parse OPML file: ${e.message}", e)
            }
        }

    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.startsWith(UTF8_BOM)) {
            return Charsets.UTF_8
        }
        if (bytes.startsWith(UTF16_LE_BOM)) {
            return Charsets.UTF_16LE
        }
        if (bytes.startsWith(UTF16_BE_BOM)) {
            return Charsets.UTF_16BE
        }

        val head = bytes.copyOfRange(0, bytes.size.coerceAtMost(XML_DECLARATION_SCAN_LENGTH))
            .toString(Charsets.ISO_8859_1)
            .replace("\u0000", "")
        val encoding = XML_ENCODING_REGEX.find(head)?.groupValues?.get(1)

        return encoding?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) {
            return false
        }
        return prefix.indices.all { index -> this[index] == prefix[index] }
    }

    override suspend fun exportFeed(
        opmlOutput: OpmlOutput,
        feedSourcesByCategory: Map<FeedSourceCategory?, List<FeedSource>>,
    ) = withContext(dispatcherProvider.io) {
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

        for ((category, feedSources) in feedSourcesByCategory) {
            if (category != null) {
                writer.writeStartElement(OpmlConstants.OUTLINE)
                writer.writeAttribute(OpmlConstants.TEXT, category.title)
                writer.writeAttribute(OpmlConstants.TITLE, category.title)
            }

            for (feedSource in feedSources) {
                writer.writeStartElement(OpmlConstants.OUTLINE)
                writer.writeAttribute(OpmlConstants.TYPE, OpmlConstants.RSS)
                writer.writeAttribute(OpmlConstants.TEXT, feedSource.title)
                writer.writeAttribute(OpmlConstants.TITLE, feedSource.title)
                writer.writeAttribute(OpmlConstants.XML_URL, feedSource.url)
                writer.writeAttribute(OpmlConstants.HTML_URL, feedSource.url)
                writer.writeEndElement()
            }

            if (category != null) {
                writer.writeEndElement()
            }
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

    companion object {
        private const val XML_DECLARATION_SCAN_LENGTH = 1024
        private val XML_ENCODING_REGEX =
            Regex("""<\?xml[^>]*encoding\s*=\s*["']([^"']+)["'][^>]*\?>""", RegexOption.IGNORE_CASE)

        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        private val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        private val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
    }
}
