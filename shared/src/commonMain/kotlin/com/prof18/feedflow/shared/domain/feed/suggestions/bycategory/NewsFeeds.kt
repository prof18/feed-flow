package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val newsCategory = SuggestedFeedCategory(
    id = "news",
    name = "News",
    icon = "📰",
    feeds = listOf(
        SuggestedFeed(
            name = "BBC News",
            url = "http://feeds.bbci.co.uk/news/rss.xml",
            description = "Breaking news from around the world",
            logoUrl = "https://www.google.com/s2/favicons?domain=bbc.co.uk&sz=64",
        ),
        SuggestedFeed(
            name = "Reuters",
            url = "https://www.reutersagency.com/feed/",
            description = "International news and business",
            logoUrl = "https://www.google.com/s2/favicons?domain=reuters.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Guardian",
            url = "https://www.theguardian.com/world/rss",
            description = "World news and opinion",
            logoUrl = "https://www.google.com/s2/favicons?domain=theguardian.com&sz=64",
        ),
        SuggestedFeed(
            name = "CNN Top Stories",
            url = "http://rss.cnn.com/rss/cnn_topstories.rss",
            description = "Latest news headlines",
            logoUrl = "https://www.google.com/s2/favicons?domain=cnn.com&sz=64",
        ),
    ),
)
