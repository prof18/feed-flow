package com.prof18.feedflow

import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.Reader

// TODO: move to common jvm package
class OPMLFeedParser(
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun parse(feed: String): List<ParsedFeedSource> = withContext(dispatcherProvider.default) {

        val feedSources = mutableListOf<ParsedFeedSource>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false

        val xmlPullParser = factory.newPullParser()
        val reader: Reader = InputStreamReader(ByteArrayInputStream(feed.trim().toByteArray()))

        xmlPullParser.setInput(reader)

        var eventType = xmlPullParser.eventType
        var categoryName: String? = null

        // Start parsing the xml
        loop@ while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when {

                    xmlPullParser.contains("outline") -> {
                        when (xmlPullParser.attributeValue("type")) {
                            "rss" -> {
                                val title = xmlPullParser.attributeValue("title")
                                val url = xmlPullParser.attributeValue("xmlUrl")
                                if (title != null && url != null) {
                                    val urlWithHttps = url
                                        .replace("http://", "https://")
                                    feedSources.add(
                                        ParsedFeedSource(
                                            url = urlWithHttps,
                                            title = title,
                                            category = categoryName,
                                        )
                                    )
                                }
                            }

                            null -> {
                                categoryName = xmlPullParser.attributeValue("title")
                            }
                        }
                    }
                }
            }
            eventType = xmlPullParser.next()
        }
        return@withContext feedSources
    }
}
