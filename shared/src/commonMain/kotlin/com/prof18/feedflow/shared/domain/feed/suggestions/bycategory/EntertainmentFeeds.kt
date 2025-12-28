package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val entertainmentFeeds = SuggestedFeedCategory(
    id = "entertainment",
    name = "Entertainment",
    icon = "🎬",
    feeds = listOf(
        SuggestedFeed(
            name = "BBC Culture",
            url = "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
            description = "Arts, culture, film, and music",
            logoUrl = "https://www.google.com/s2/favicons?domain=bbc.co.uk&sz=64",
        ),
        SuggestedFeed(
            name = "Billboard",
            url = "https://www.billboard.com/feed",
            description = "Music charts, news, photos and video",
            logoUrl = "https://www.google.com/s2/favicons?domain=billboard.com&sz=64",
        ),
        SuggestedFeed(
            name = "IndieWire",
            url = "https://www.indiewire.com/feed/",
            description = "Film industry news and independent cinema",
            logoUrl = "https://www.google.com/s2/favicons?domain=indiewire.com&sz=64",
        ),
        SuggestedFeed(
            name = "Mashable",
            url = "http://feeds.mashable.com/Mashable",
            description = "Digital culture and entertainment",
            logoUrl = "https://www.google.com/s2/favicons?domain=mashable.com&sz=64",
        ),
        SuggestedFeed(
            name = "Pitchfork",
            url = "https://pitchfork.com/feed/feed-news/rss",
            description = "The most trusted voice in music",
            logoUrl = "https://www.google.com/s2/favicons?domain=pitchfork.com&sz=64",
        ),
        SuggestedFeed(
            name = "Polygon",
            url = "https://www.polygon.com/rss/index.xml",
            description = "Gaming news and reviews",
            logoUrl = "https://www.google.com/s2/favicons?domain=polygon.com&sz=64",
        ),
        SuggestedFeed(
            name = "PopCulture.com",
            url = "https://popculture.com/feed/rss/",
            description = "Celebrity and entertainment news",
            logoUrl = "https://www.google.com/s2/favicons?domain=popculture.com&sz=64",
        ),
        SuggestedFeed(
            name = "Rolling Stone",
            url = "https://www.rollingstone.com/feed/",
            description = "Music, pop culture, and politics",
            logoUrl = "https://www.google.com/s2/favicons?domain=rollingstone.com&sz=64",
        ),
        SuggestedFeed(
            name = "ScreenRant",
            url = "https://screenrant.com/feed/",
            description = "Movie news, reviews, and theories",
            logoUrl = "https://www.google.com/s2/favicons?domain=screenrant.com&sz=64",
        ),
        SuggestedFeed(
            name = "Vanity Fair",
            url = "https://www.vanityfair.com/feed/rss",
            description = "Culture, fashion, and celebrity",
            logoUrl = "https://www.google.com/s2/favicons?domain=vanityfair.com&sz=64",
        ),
        SuggestedFeed(
            name = "Variety",
            url = "https://variety.com/feed/",
            description = "Entertainment industry news",
            logoUrl = "https://www.google.com/s2/favicons?domain=variety.com&sz=64",
        ),
    ),
)
