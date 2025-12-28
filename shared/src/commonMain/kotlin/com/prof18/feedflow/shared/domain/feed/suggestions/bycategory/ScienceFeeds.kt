package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val scienceCategory = SuggestedFeedCategory(
    id = "science",
    name = "Science",
    icon = "🔬",
    feeds = listOf(
        SuggestedFeed(
            name = "NASA Breaking News",
            url = "https://www.nasa.gov/rss/dyn/breaking_news.rss",
            description = "Latest NASA news",
            logoUrl = "https://www.google.com/s2/favicons?domain=nasa.gov&sz=64",
        ),
        SuggestedFeed(
            name = "Science Daily",
            url = "https://www.sciencedaily.com/rss/all.xml",
            description = "Latest science news",
            logoUrl = "https://www.google.com/s2/favicons?domain=sciencedaily.com&sz=64",
        ),
        SuggestedFeed(
            name = "Nature",
            url = "https://www.nature.com/nature.rss",
            description = "International journal of science",
            logoUrl = "https://www.google.com/s2/favicons?domain=nature.com&sz=64",
        ),
        SuggestedFeed(
            name = "Scientific American",
            url = "http://rss.sciam.com/ScientificAmerican-Global",
            description = "Science news and technology updates",
            logoUrl = "https://www.google.com/s2/favicons?domain=scientificamerican.com&sz=64",
        ),
    ),
)
