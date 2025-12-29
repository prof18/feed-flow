package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val businessFeeds = SuggestedFeedCategory(
    id = "business",
    name = "Business",
    icon = "💼",
    feeds = listOf(
        SuggestedFeed(
            name = "Business Insider",
            url = "https://feeds2.feedburner.com/businessinsider",
            description = "Markets and tech business news",
            logoUrl = "https://www.google.com/s2/favicons?domain=businessinsider.com&sz=64",
        ),
        SuggestedFeed(
            name = "Entrepreneur",
            url = "https://www.entrepreneur.com/latest.rss",
            description = "Small business and startup advice",
            logoUrl = "https://www.google.com/s2/favicons?domain=entrepreneur.com&sz=64",
        ),
        SuggestedFeed(
            name = "Fast Company",
            url = "https://feeds.feedburner.com/fastcompany/headlines",
            description = "Innovation and business design",
            logoUrl = "https://www.google.com/s2/favicons?domain=fastcompany.com&sz=64",
        ),
        SuggestedFeed(
            name = "Financial Times",
            url = "https://www.ft.com/rss/home",
            description = "International business and economic news",
            logoUrl = "https://www.google.com/s2/favicons?domain=ft.com&sz=64",
        ),
        SuggestedFeed(
            name = "Fortune",
            url = "https://fortune.com/feed",
            description = "Business leadership and strategy",
            logoUrl = "https://www.google.com/s2/favicons?domain=fortune.com&sz=64",
        ),
        SuggestedFeed(
            name = "Harvard Business Review",
            url = "http://feeds.harvardbusiness.org/harvardbusiness",
            description = "Ideas and advice for leaders",
            logoUrl = "https://www.google.com/s2/favicons?domain=hbr.org&sz=64",
        ),
        SuggestedFeed(
            name = "MarketWatch",
            url = "https://feeds.marketwatch.com/marketwatch/topstories/",
            description = "Stock market and financial news",
            logoUrl = "https://www.google.com/s2/favicons?domain=marketwatch.com&sz=64",
        ),
        SuggestedFeed(
            name = "Not Boring",
            url = "https://www.notboring.co/feed",
            description = "Business strategy newsletter",
            logoUrl = "https://www.google.com/s2/favicons?domain=notboring.co&sz=64",
        ),
        SuggestedFeed(
            name = "TIME Business",
            url = "https://feeds.feedburner.com/time/business",
            description = "Business and economic coverage",
            logoUrl = "https://www.google.com/s2/favicons?domain=time.com&sz=64",
        ),
        SuggestedFeed(
            name = "Wall Street Journal",
            url = "https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml",
            description = "US business news",
            logoUrl = "https://www.google.com/s2/favicons?domain=wsj.com&sz=64",
        ),
        SuggestedFeed(
            name = "Yahoo Finance",
            url = "https://finance.yahoo.com/rss/",
            description = "Market news and investing insights",
            logoUrl = "https://www.google.com/s2/favicons?domain=yahoo.com&sz=64",
        ),
    ),
)
