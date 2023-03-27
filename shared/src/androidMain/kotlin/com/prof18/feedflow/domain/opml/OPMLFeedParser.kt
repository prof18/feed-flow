package com.prof18.feedflow.domain.opml

import android.content.Context
import com.prof18.feedflow.attributeValue
import com.prof18.feedflow.contains
import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.Reader

internal actual class OPMLFeedParser(
    private val dispatcherProvider: DispatcherProvider,
) {
    actual suspend fun parse(opmlInput: OPMLInput): List<ParsedFeedSource> = withContext(dispatcherProvider.io) {
        val inputStream = opmlInput.inputStream
        val feedSources = mutableListOf<ParsedFeedSource>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false

        val xmlPullParser = factory.newPullParser()
        val reader: Reader = InputStreamReader(inputStream)

        xmlPullParser.setInput(reader)

        var eventType = xmlPullParser.eventType
        var categoryName: String? = null

        // Start parsing the xml
        loop@ while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when {

                    xmlPullParser.contains(OPMLConstants.OUTLINE) -> {
                        when (xmlPullParser.attributeValue(OPMLConstants.TYPE)) {
                            OPMLConstants.RSS -> {
                                val builder = ParsedFeedSource.Builder().apply {
                                    title(xmlPullParser.attributeValue(OPMLConstants.TITLE))
                                    url(xmlPullParser.attributeValue(OPMLConstants.XML_URL))
                                    category(categoryName)
                                }
                                builder.build()?.let {
                                    feedSources.add(it)
                                }
                            }

                            null -> {
                                categoryName = xmlPullParser.attributeValue(OPMLConstants.TITLE)
                            }
                        }
                    }
                }
            }
            eventType = xmlPullParser.next()
        }
        inputStream?.close()
        return@withContext feedSources
    }
}
