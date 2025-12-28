package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val newsFeeds = SuggestedFeedCategory(
    id = "news",
    name = "News",
    icon = "📰",
    feeds = listOf(
        SuggestedFeed(
            name = "Al Jazeera",
            url = "https://www.aljazeera.com/xml/rss/all.xml",
            description = "International news from Middle East perspective",
            logoUrl = "https://www.google.com/s2/favicons?domain=aljazeera.com&sz=64",
        ),
        SuggestedFeed(
            name = "BBC News - World",
            url = "https://feeds.bbci.co.uk/news/world/rss.xml",
            description = "Global news from the BBC",
            logoUrl = "https://www.google.com/s2/favicons?domain=bbc.co.uk&sz=64",
        ),
        SuggestedFeed(
            name = "CNBC",
            url = "https://www.cnbc.com/id/100003114/device/rss/rss.html",
            description = "Top news in business and finance",
            logoUrl = "https://www.google.com/s2/favicons?domain=cnbc.com&sz=64",
        ),
        SuggestedFeed(
            name = "NPR News",
            url = "https://feeds.npr.org/1001/rss.xml",
            description = "US and international news from NPR",
            logoUrl = "https://www.google.com/s2/favicons?domain=npr.org&sz=64",
        ),
        SuggestedFeed(
            name = "Sky News - World",
            url = "https://feeds.skynews.com/feeds/rss/world.xml",
            description = "Breaking international news",
            logoUrl = "https://www.google.com/s2/favicons?domain=skynews.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Guardian – World",
            url = "https://www.theguardian.com/world/rss",
            description = "Independent global news and reporting",
            logoUrl = "https://www.google.com/s2/favicons?domain=theguardian.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Independent - News",
            url = "https://www.independent.co.uk/news/rss",
            description = "UK independent news coverage",
            logoUrl = "https://www.google.com/s2/favicons?domain=independent.co.uk&sz=64",
        ),
        SuggestedFeed(
            name = "The New York Times ",
            url = "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
            description = "Global news and in-depth analysis",
            logoUrl = "https://www.google.com/s2/favicons?domain=nytimes.com&sz=64",
        ),
        SuggestedFeed(
            name = "The New York Times - World",
            url = "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
            description = "Global news and in-depth analysis",
            logoUrl = "https://www.google.com/s2/favicons?domain=nytimes.com&sz=64",
        ),
        SuggestedFeed(
            name = "The New Yorker - News",
            url = "https://www.newyorker.com/feed/news",
            description = "News with cultural commentary",
            logoUrl = "https://www.google.com/s2/favicons?domain=newyorker.com&sz=64",
        ),
        SuggestedFeed(
            name = "TIME - World",
            url = "https://feeds.feedburner.com/time/world",
            description = "Global news and current affairs",
            logoUrl = "https://www.google.com/s2/favicons?domain=time.com&sz=64",
        ),
        SuggestedFeed(
            name = "Wall Street Journal",
            url = "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
            description = "Real-time updates on the stock market",
            logoUrl = "https://www.google.com/s2/favicons?domain=wsj.com&sz=64",
        ),
    ),
)
