package com.prof18.feedflow.shared.test.generators

import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssImage
import com.prof18.rssparser.model.RssItem

object RssItemGenerator {
    fun rssItem(
        guid: String? = "rss-item-guid",
        title: String? = "RSS item title",
        author: String? = "RSS item author",
        link: String? = "https://example.com/article",
        pubDate: String? = "Mon, 01 Jan 2024 12:00:00 +0000",
        description: String? = "RSS item description",
        content: String? = "RSS item content",
        image: String? = "https://example.com/image.jpg",
        audio: String? = null,
        video: String? = null,
        sourceName: String? = "Example source",
        sourceUrl: String? = "https://example.com",
        categories: List<String> = emptyList(),
        commentsUrl: String? = "https://example.com/comments",
    ): RssItem = RssItem(
        guid = guid,
        title = title,
        author = author,
        link = link,
        pubDate = pubDate,
        description = description,
        content = content,
        image = image,
        audio = audio,
        video = video,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        categories = categories,
        itunesItemData = null,
        commentsUrl = commentsUrl,
        youtubeItemData = null,
        rawEnclosure = null,
    )
}

object RssChannelGenerator {
    fun rssChannel(
        title: String? = "RSS channel title",
        link: String? = "https://example.com",
        description: String? = "RSS channel description",
        image: RssImage? = null,
        lastBuildDate: String? = "Mon, 01 Jan 2024 12:00:00 +0000",
        updatePeriod: String? = "daily",
        items: List<RssItem> = emptyList(),
    ): RssChannel = RssChannel(
        title = title,
        link = link,
        description = description,
        image = image,
        lastBuildDate = lastBuildDate,
        updatePeriod = updatePeriod,
        items = items,
        itunesChannelData = null,
        youtubeChannelData = null,
    )
}
