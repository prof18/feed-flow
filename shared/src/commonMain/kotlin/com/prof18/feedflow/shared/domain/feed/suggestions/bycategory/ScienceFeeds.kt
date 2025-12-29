package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val scienceFeeds = SuggestedFeedCategory(
    id = "science",
    name = "Science",
    icon = "🧠",
    feeds = listOf(
        SuggestedFeed(
            name = "Ars Technica Space",
            url = "https://arstechnica.com/author/ericberger/feed/",
            description = "Space and rocket science news",
            logoUrl = "https://www.google.com/s2/favicons?domain=arstechnica.com&sz=64",
        ),
        SuggestedFeed(
            name = "Astronomy Magazine",
            url = "https://astronomy.com/rss",
            description = "Astronomy news and features",
            logoUrl = "https://www.google.com/s2/favicons?domain=astronomy.com&sz=64",
        ),
        SuggestedFeed(
            name = "CleanTechnica",
            url = "https://cleantechnica.com/feed/",
            description = "Clean energy and technology",
            logoUrl = "https://www.google.com/s2/favicons?domain=cleantechnica.com&sz=64",
        ),
        SuggestedFeed(
            name = "ESA",
            url = "http://www.esa.int/rssfeed/Our_Activities/Space_News",
            description = "European Space Agency news",
            logoUrl = "https://www.google.com/s2/favicons?domain=esa.int&sz=64",
        ),
        SuggestedFeed(
            name = "Grist",
            url = "https://grist.org/feed/",
            description = "Climate news and solutions",
            logoUrl = "https://www.google.com/s2/favicons?domain=grist.org&sz=64",
        ),
        SuggestedFeed(
            name = "NASA",
            url = "https://www.nasa.gov/rss/dyn/breaking_news.rss",
            description = "NASA breaking news",
            logoUrl = "https://www.google.com/s2/favicons?domain=nasa.gov&sz=64",
        ),
        SuggestedFeed(
            name = "Nature",
            url = "https://www.nature.com/nature.rss",
            description = "International journal of science",
            logoUrl = "https://www.google.com/s2/favicons?domain=nature.com&sz=64",
        ),
        SuggestedFeed(
            name = "New Scientist",
            url = "https://www.newscientist.com/feed/home/",
            description = "Science and technology news explained",
            logoUrl = "https://www.google.com/s2/favicons?domain=newscientist.com&sz=64",
        ),
        SuggestedFeed(
            name = "Popular Science",
            url = "https://www.popsci.com/feed/",
            description = "Science and technology news",
            logoUrl = "https://www.google.com/s2/favicons?domain=popsci.com&sz=64",
        ),
        SuggestedFeed(
            name = "ScienceDaily",
            url = "https://www.sciencedaily.com/rss/all.xml",
            description = "Latest science news across all disciplines",
            logoUrl = "https://www.google.com/s2/favicons?domain=sciencedaily.com&sz=64",
        ),
        SuggestedFeed(
            name = "Space.com",
            url = "https://www.space.com/feeds/all",
            description = "Space and astronomy news",
            logoUrl = "https://www.google.com/s2/favicons?domain=space.com&sz=64",
        ),
        SuggestedFeed(
            name = "The British Psychological Society",
            url = "https://www.bps.org.uk/feed",
            description = "Psychology research and news",
            logoUrl = "https://www.google.com/s2/favicons?domain=bps.org.uk&sz=64",
        ),
        SuggestedFeed(
            name = "Universe Today",
            url = "https://www.universetoday.com/feed/",
            description = "Space exploration updates",
            logoUrl = "https://www.google.com/s2/favicons?domain=universetoday.com&sz=64",
        ),
        SuggestedFeed(
            name = "WIRED Science",
            url = "https://www.wired.com/feed/category/science/latest/rss",
            description = "Science and technology features",
            logoUrl = "https://www.google.com/s2/favicons?domain=wired.com&sz=64",
        ),
    ),
)
