package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val gamingFeeds = SuggestedFeedCategory(
    id = "gaming",
    name = "Gaming",
    icon = "🎮",
    feeds = listOf(
        SuggestedFeed(
            name = "Eurogamer",
            url = "https://www.eurogamer.net/?format=rss",
            description = "European gaming news",
            logoUrl = "https://www.google.com/s2/favicons?domain=eurogamer.net&sz=64",
        ),
        SuggestedFeed(
            name = "GameSpot",
            url = "https://www.gamespot.com/feeds/mashup/",
            description = "Gaming news and videos",
            logoUrl = "https://www.google.com/s2/favicons?domain=gamespot.com&sz=64",
        ),
        SuggestedFeed(
            name = "IGN",
            url = "https://feeds.ign.com/ign/news",
            description = "Video game news, reviews, and walkthroughs",
            logoUrl = "https://www.google.com/s2/favicons?domain=ign.com&sz=64",
        ),
        SuggestedFeed(
            name = "PC Gamer",
            url = "https://www.pcgamer.com/rss/",
            description = "PC gaming news and reviews",
            logoUrl = "https://www.google.com/s2/favicons?domain=pcgamer.com&sz=64",
        ),
    ),
)
