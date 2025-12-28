package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val designCategory = SuggestedFeedCategory(
    id = "design",
    name = "Design",
    icon = "🎨",
    feeds = listOf(
        SuggestedFeed(
            name = "Smashing Magazine",
            url = "https://www.smashingmagazine.com/feed/",
            description = "Web design and development",
            logoUrl = "https://www.google.com/s2/favicons?domain=smashingmagazine.com&sz=64",
        ),
        SuggestedFeed(
            name = "Designer News",
            url = "https://www.designernews.co/?format=rss",
            description = "Design community news",
            logoUrl = "https://www.google.com/s2/favicons?domain=designernews.co&sz=64",
        ),
        SuggestedFeed(
            name = "Dribbble",
            url = "https://dribbble.com/shots/popular.rss",
            description = "Design inspiration",
            logoUrl = "https://www.google.com/s2/favicons?domain=dribbble.com&sz=64",
        ),
        SuggestedFeed(
            name = "Awwwards",
            url = "https://www.awwwards.com/blog/feed/",
            description = "Web design trends and inspiration",
            logoUrl = "https://www.google.com/s2/favicons?domain=awwwards.com&sz=64",
        ),
    ),
)
