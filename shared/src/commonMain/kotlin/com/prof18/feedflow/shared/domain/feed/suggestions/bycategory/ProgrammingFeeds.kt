package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val developmentFeeds = SuggestedFeedCategory(
    id = "development",
    name = "Development",
    icon = "👩‍💻",
    feeds = listOf(
        SuggestedFeed(
            name = "Android Developers",
            url = "https://android-developers.googleblog.com/feeds/posts/default?alt=rss",
            description = "The latest Android news and tips",
            logoUrl = "https://www.google.com/s2/favicons?domain=developer.android.com&sz=64",
        ),
        SuggestedFeed(
            name = "CSS-Tricks",
            url = "https://css-tricks.com/feed/",
            description = "Tips, tricks, and techniques on using CSS",
            logoUrl = "https://www.google.com/s2/favicons?domain=css-tricks.com&sz=64",
        ),
        SuggestedFeed(
            name = "Hacker News",
            url = "https://news.ycombinator.com/rss",
            description = "Links for the intellectually curious, ranked by readers",
            logoUrl = "https://www.google.com/s2/favicons?domain=news.ycombinator.com&sz=64",
        ),
        SuggestedFeed(
            name = "JavaScript Weekly",
            url = "https://javascriptweekly.com/rss",
            description = "A newsletter of JavaScript articles, news and cool projects",
            logoUrl = "https://www.google.com/s2/favicons?domain=javascriptweekly.com&sz=64",
        ),
        SuggestedFeed(
            name = "Kotlin Blog",
            url = "https://blog.jetbrains.com/kotlin/feed/",
            description = "Official Kotlin programming language blog",
            logoUrl = "https://www.google.com/s2/favicons?domain=jetbrains.com&sz=64",
        ),
        SuggestedFeed(
            name = "Product Hunt",
            url = "https://www.producthunt.com/feed",
            description = "Latest product launches",
            logoUrl = "https://www.google.com/s2/favicons?domain=producthunt.com&sz=64",
        ),
    ),
)
