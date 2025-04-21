package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ReaderModeExtractor internal constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val htmlRetriever: HtmlRetriever,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun extractReaderContent(urlInfo: FeedItemUrlInfo): ReaderModeData? = withContext(dispatcherProvider.io) {
        val html = htmlRetriever.retrieveHtml(urlInfo.url) ?: return@withContext null

        val doc = Jsoup.parse(html)
        val ogImage = getOgImage(doc, urlInfo.url)
        if (ogImage != null) {
            // Sometimes the image isn't present in the content, but if it is,
            // better to avoid duplicates in the content
            removeFirstImageTagByUrl(doc, ogImage)
        }

        val modifiedHtml = doc.html()

        return@withContext ReaderModeData(
            id = FeedItemId(urlInfo.id),
            content = modifiedHtml,
            url = urlInfo.url,
            fontSize = settingsRepository.getReaderModeFontSize(),
            isBookmarked = urlInfo.isBookmarked,
            heroImageUrl = ogImage,
            title = null,
        )
    }

    private fun getOgImage(doc: Document, baseUrl: String): String? {
        return doc.select("meta[property=og:image]")
            .firstOrNull()
            ?.attr("content")
            ?.let { content ->
                if (content.startsWith("//") || content.startsWith("/")) {
                    val baseUrlWithoutProtocol = baseUrl.replace(Regex("^https?://"), "")
                    "https://$baseUrlWithoutProtocol$content"
                } else if (!content.startsWith("http")) {
                    "$baseUrl/$content"
                } else {
                    content
                }
            }
    }

    private fun removeFirstImageTagByUrl(doc: Document, imageUrl: String) {
        val baseImageUrl = imageUrl.split("?")[0]
        val img = doc.select("img").firstOrNull { img ->
            val imgSrc = img.attr("src")
            val imgSrcset = img.attr("srcset")

            // Check both src and srcset attributes
            val srcMatches = imgSrc.split("?")[0] == baseImageUrl
            val srcsetMatches = imgSrcset.split(",").any { srcsetItem ->
                // srcset items are like "url 320w" or "url 2x"
                val srcsetUrl = srcsetItem.trim().split(" ")[0]
                srcsetUrl.split("?")[0] == baseImageUrl
            }

            srcMatches || srcsetMatches
        }
        if (img != null) {
            val figure = img.parents().firstOrNull { it.tagName() == "figure" }
            if (figure != null) {
                figure.remove()
            } else {
                img.remove()
            }
        }
    }
}
