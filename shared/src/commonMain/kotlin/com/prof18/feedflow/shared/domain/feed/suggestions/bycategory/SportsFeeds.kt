package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val sportsFeeds = SuggestedFeedCategory(
    id = "sports",
    name = "Sports",
    icon = "🏆",
    feeds = listOf(
        SuggestedFeed(
            name = "Autosport",
            url = "https://www.autosport.com/rss/all/news/",
            description = "Formula 1 and motorsport news",
            logoUrl = "https://www.google.com/s2/favicons?domain=autosport.com&sz=64",
        ),
        SuggestedFeed(
            name = "BBC Sport",
            url = "https://feeds.bbci.co.uk/sport/rss.xml",
            description = "Global sports coverage from the BBC",
            logoUrl = "https://www.google.com/s2/favicons?domain=bbci.co.uk&sz=64",
        ),
        SuggestedFeed(
            name = "CaughtOffside",
            url = "https://www.caughtoffside.com/feed/",
            description = "Football news, transfer rumors, and match commentary",
            logoUrl = "https://www.google.com/s2/favicons?domain=caughtoffside.com&sz=64",
        ),
        SuggestedFeed(
            name = "Defector",
            url = "https://defector.com/feed",
            description = "Sports and culture writing with independent editorial voice",
            logoUrl = "https://www.google.com/s2/favicons?domain=defector.com&sz=64",
        ),
        SuggestedFeed(
            name = "ESPN Top Headlines",
            url = "https://www.espn.com/espn/rss/news",
            description = "Top sports news and scores",
            logoUrl = "https://www.google.com/s2/favicons?domain=espn.com&sz=64",
        ),
        SuggestedFeed(
            name = "FiveThirtyEight - Sports",
            url = "https://fivethirtyeight.com/sports/feed/",
            description = "Data-driven analysis of sports trends and outcomes",
            logoUrl = "https://www.google.com/s2/favicons?domain=fivethirtyeight.com&sz=64",
        ),
        SuggestedFeed(
            name = "Motorsport",
            url = "https://www.motorsport.com/rss/all/news/",
            description = "Racing and motorsport coverage",
            logoUrl = "https://www.google.com/s2/favicons?domain=motorsport.com&sz=64",
        ),
        SuggestedFeed(
            name = "SB Nation",
            url = "https://www.sbnation.com/rss/index.xml",
            description = "Team-focused sports news across major leagues",
            logoUrl = "https://www.google.com/s2/favicons?domain=sbnation.com&sz=64",
        ),
        SuggestedFeed(
            name = "Sky Sports",
            url = "https://www.skysports.com/rss/12040",
            description = "UK sports news and live scores",
            logoUrl = "https://www.google.com/s2/favicons?domain=skysports.com&sz=64",
        ),
        SuggestedFeed(
            name = "Sports Illustrated",
            url = "https://www.si.com/feed",
            description = "In-depth sports journalism",
            logoUrl = "https://www.google.com/s2/favicons?domain=si.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Guardian - Sports",
            url = "https://www.theguardian.com/uk/sport/rss",
            description = "International sports news",
            logoUrl = "https://www.google.com/s2/favicons?domain=theguardian.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Independent - Sports",
            url = "https://www.the-independent.com/sport/rss",
            description = "Sports news and features",
            logoUrl = "https://www.google.com/s2/favicons?domain=the-independent.com&sz=64",
        ),
    ),
)
