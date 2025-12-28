package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val programmingCategory = SuggestedFeedCategory(
    id = "programming",
    name = "Programming",
    icon = "👨‍💻",
    feeds = listOf(
        SuggestedFeed(
            name = "CSS Tricks",
            url = "https://css-tricks.com/feed/",
            description = "Tips, tricks, and techniques on using CSS",
            logoUrl = "https://www.google.com/s2/favicons?domain=css-tricks.com&sz=64",
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
            logoUrl = "https://www.google.com/s2/favicons?domain=kotlinlang.org&sz=64",
        ),
        SuggestedFeed(
            name = "Android Developers Blog",
            url = "https://android-developers.googleblog.com/feeds/posts/default",
            description = "The latest Android and Google Play news",
            logoUrl = "https://www.google.com/s2/favicons?domain=developer.android.com&sz=64",
        ),
    ),
)
