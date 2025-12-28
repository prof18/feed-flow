package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val businessCategory = SuggestedFeedCategory(
    id = "business",
    name = "Business",
    icon = "💼",
    feeds = listOf(
        SuggestedFeed(
            name = "Harvard Business Review",
            url = "https://feeds.hbr.org/harvardbusiness",
            description = "Management and leadership insights",
            logoUrl = "https://www.google.com/s2/favicons?domain=hbr.org&sz=64",
        ),
        SuggestedFeed(
            name = "Forbes",
            url = "https://www.forbes.com/business/feed/",
            description = "Business news and financial updates",
            logoUrl = "https://www.google.com/s2/favicons?domain=forbes.com&sz=64",
        ),
        SuggestedFeed(
            name = "Entrepreneur",
            url = "https://www.entrepreneur.com/latest.rss",
            description = "Entrepreneurship and startup news",
            logoUrl = "https://www.google.com/s2/favicons?domain=entrepreneur.com&sz=64",
        ),
        SuggestedFeed(
            name = "Bloomberg",
            url = "https://www.bloomberg.com/feed/podcast/what-goes-up.xml",
            description = "Business and financial news",
            logoUrl = "https://www.google.com/s2/favicons?domain=bloomberg.com&sz=64",
        ),
    ),
)
