package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val technologyFeeds = SuggestedFeedCategory(
    id = "technology",
    name = "Technology",
    icon = "💻",
    feeds = listOf(
        SuggestedFeed(
            name = "Ars Technica",
            url = "https://feeds.arstechnica.com/arstechnica/index",
            description = "Deep dives into software, hardware, and science",
            logoUrl = "https://www.google.com/s2/favicons?domain=arstechnica.com&sz=64",
        ),
        SuggestedFeed(
            name = "Daring Fireball",
            url = "https://daringfireball.net/feeds/articles",
            description = "Apple and tech commentary",
            logoUrl = "https://www.google.com/s2/favicons?domain=daringfireball.net&sz=64",
        ),
        SuggestedFeed(
            name = "Gizmodo",
            url = "https://gizmodo.com/rss",
            description = "Design, tech, and science news",
            logoUrl = "https://www.google.com/s2/favicons?domain=gizmodo.com&sz=64",
        ),
        SuggestedFeed(
            name = "Hacker News",
            url = "https://news.ycombinator.com/rss",
            description = "Top discussions from the tech and startup community",
            logoUrl = "https://www.google.com/s2/favicons?domain=ycombinator.com&sz=64",
        ),
        SuggestedFeed(
            name = "MIT News",
            url = "https://news.mit.edu/rss/feed",
            description = "Research and innovation from MIT",
            logoUrl = "https://www.google.com/s2/favicons?domain=mit.edu&sz=64",
        ),
        SuggestedFeed(
            name = "TechCrunch",
            url = "https://techcrunch.com/feed/",
            description = "Startups, technology news, and product launches",
            logoUrl = "https://www.google.com/s2/favicons?domain=techcrunch.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Verge",
            url = "https://www.theverge.com/rss/index.xml",
            description = "Technology, science, and culture",
            logoUrl = "https://www.google.com/s2/favicons?domain=theverge.com&sz=64",
        ),
        SuggestedFeed(
            name = "Wired",
            url = "https://www.wired.com/feed/rss",
            description = "How technology is changing every aspect of our lives",
            logoUrl = "https://www.google.com/s2/favicons?domain=wired.com&sz=64",
        ),
    ),
)
