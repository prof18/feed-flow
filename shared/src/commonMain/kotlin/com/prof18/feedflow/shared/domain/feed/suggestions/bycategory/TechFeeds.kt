package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val techCategory = SuggestedFeedCategory(
    id = "tech",
    name = "Technology",
    icon = "💻",
    feeds = listOf(
        SuggestedFeed(
            name = "TechCrunch",
            url = "https://techcrunch.com/feed/",
            description = "The latest technology news and information on startups",
            logoUrl = "https://www.google.com/s2/favicons?domain=techcrunch.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Verge",
            url = "https://www.theverge.com/rss/index.xml",
            description = "Technology, science, art, and culture",
            logoUrl = "https://www.google.com/s2/favicons?domain=theverge.com&sz=64",
        ),
        SuggestedFeed(
            name = "Hacker News",
            url = "https://hnrss.org/frontpage",
            description = "News for hackers",
            logoUrl = "https://www.google.com/s2/favicons?domain=news.ycombinator.com&sz=64",
        ),
        SuggestedFeed(
            name = "Ars Technica",
            url = "https://feeds.arstechnica.com/arstechnica/index",
            description = "Technology news and analysis",
            logoUrl = "https://www.google.com/s2/favicons?domain=arstechnica.com&sz=64",
        ),
    ),
)
