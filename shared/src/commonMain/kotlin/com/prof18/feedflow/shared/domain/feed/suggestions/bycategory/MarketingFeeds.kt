package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val marketingFeeds = SuggestedFeedCategory(
    id = "marketing",
    name = "Marketing",
    icon = "📈",
    feeds = listOf(
        SuggestedFeed(
            name = "Adweek",
            url = "http://feeds.feedburner.com/Adfreak",
            description = "Marketing news and insights from Adweek",
            logoUrl = "https://www.google.com/s2/favicons?domain=adweek.com&sz=64",
        ),
        SuggestedFeed(
            name = "Ahrefs Blog",
            url = "https://ahrefs.com/blog/feed/",
            description = "SEO, content strategy, and search marketing",
            logoUrl = "https://www.google.com/s2/favicons?domain=ahrefs.com&sz=64",
        ),
        SuggestedFeed(
            name = "Annhandley",
            url = "http://feeds.feedburner.com/ANNARCHY",
            description = "Marketing news and insights from Annhandley",
            logoUrl = "https://www.google.com/s2/favicons?domain=annhandley.com&sz=64",
        ),
        SuggestedFeed(
            name = "AWeber",
            url = "http://blog.aweber.com/feed",
            description = "Marketing news and insights from AWeber",
            logoUrl = "https://www.google.com/s2/favicons?domain=aweber.com&sz=64",
        ),
        SuggestedFeed(
            name = "Backlinko",
            url = "https://backlinko.com/feed",
            description = "Search engine optimization and organic growth tactics",
            logoUrl = "https://www.google.com/s2/favicons?domain=backlinko.com&sz=64",
        ),
        SuggestedFeed(
            name = "Bluleadz",
            url = "https://www.bluleadz.com/blog/rss.xml",
            description = "Marketing news and insights from Bluleadz",
            logoUrl = "https://www.google.com/s2/favicons?domain=bluleadz.com&sz=64",
        ),
        SuggestedFeed(
            name = "Brandingstrategyinsider",
            url = "http://feeds.feedburner.com/BrandingStrategyInsider",
            description = "Marketing news and insights from Brandingstrategyinsider",
            logoUrl = "https://www.google.com/s2/favicons?domain=brandingstrategyinsider.com&sz=64",
        ),
        SuggestedFeed(
            name = "Chiefmartec",
            url = "http://feeds.feedburner.com/ChiefMarketingTechnologist",
            description = "Marketing news and insights from Chiefmartec",
            logoUrl = "https://www.google.com/s2/favicons?domain=chiefmartec.com&sz=64",
        ),
        SuggestedFeed(
            name = "Copyhackers",
            url = "http://copyhackers.com/feed/",
            description = "Conversion copywriting and messaging strategy",
            logoUrl = "https://www.google.com/s2/favicons?domain=copyhackers.com&sz=64",
        ),
        SuggestedFeed(
            name = "David Meerman Scott",
            url = "https://www.davidmeermanscott.com/blog/rss.xml",
            description = "Marketing news and insights from David Meerman Scott",
            logoUrl = "https://www.google.com/s2/favicons?domain=davidmeermanscott.com&sz=64",
        ),
        SuggestedFeed(
            name = "Digiday",
            url = "https://digiday.com/feed/",
            description = "Media, advertising, and digital marketing industry news",
            logoUrl = "https://www.google.com/s2/favicons?domain=digiday.com&sz=64",
        ),
        SuggestedFeed(
            name = "Duct Tape Marketing",
            url = "https://ducttapemarketing.com/feed/",
            description = "Practical marketing frameworks for small businesses",
            logoUrl = "https://www.google.com/s2/favicons?domain=ducttapemarketing.com&sz=64",
        ),
        SuggestedFeed(
            name = "Hootsuite Blog",
            url = "https://blog.hootsuite.com/feed/",
            description = "Social media strategy and platform updates",
            logoUrl = "https://www.google.com/s2/favicons?domain=hootsuite.com&sz=64",
        ),
        SuggestedFeed(
            name = "MarTech Series",
            url = "http://martechseries.com/feed/",
            description = "Marketing news and insights from MarTech Series",
            logoUrl = "https://www.google.com/s2/favicons?domain=martechseries.com&sz=64",
        ),
        SuggestedFeed(
            name = "MarTech Zone",
            url = "https://martech.zone/feed/",
            description = "Digital marketing tactics and martech tools",
            logoUrl = "https://www.google.com/s2/favicons?domain=martech.zone&sz=64",
        ),
        SuggestedFeed(
            name = "Moz",
            url = "http://feeds.feedburner.com/seomoz",
            description = "Marketing news and insights from Moz",
            logoUrl = "https://www.google.com/s2/favicons?domain=moz.com&sz=64",
        ),
        SuggestedFeed(
            name = "Search Engine Journal",
            url = "https://rss.searchenginejournal.com/",
            description = "SEO, PPC, and search industry news",
            logoUrl = "https://www.google.com/s2/favicons?domain=searchenginejournal.com&sz=64",
        ),
        SuggestedFeed(
            name = "Search Engine Roundtable",
            url = "http://feeds.seroundtable.com/SearchEngineRoundtable1",
            description = "Marketing news and insights from Search Engine Roundtable",
            logoUrl = "https://www.google.com/s2/favicons?domain=seroundtable.com&sz=64",
        ),
        SuggestedFeed(
            name = "Semrush",
            url = "https://www.semrush.com:443/blog/feed/",
            description = "Marketing news and insights from Semrush",
            logoUrl = "https://www.google.com/s2/favicons?domain=semrush.com&sz=64",
        ),
        SuggestedFeed(
            name = "SEO Book",
            url = "http://www.seobook.com/rss.xml",
            description = "Marketing news and insights from SEO Book",
            logoUrl = "https://www.google.com/s2/favicons?domain=seobook.com&sz=64",
        ),
        SuggestedFeed(
            name = "Social Media Today",
            url = "https://www.socialmediatoday.com/feeds/news/",
            description = "Marketing news and insights from Social Media Today",
            logoUrl = "https://www.google.com/s2/favicons?domain=socialmediatoday.com&sz=64",
        ),
        SuggestedFeed(
            name = "Social Media Explorer",
            url = "http://feeds.feedburner.com/SocialMediaExplorer",
            description = "Marketing news and insights from Socialmediaexplorer",
            logoUrl = "https://www.google.com/s2/favicons?domain=socialmediaexplorer.com&sz=64",
        ),
        SuggestedFeed(
            name = "Sproutsocial",
            url = "http://feeds.feedburner.com/SproutInsights",
            description = "Marketing news and insights from Sproutsocial",
            logoUrl = "https://www.google.com/s2/favicons?domain=sproutsocial.com&sz=64",
        ),
        SuggestedFeed(
            name = "Web Strategist",
            url = "https://web-strategist.com/blog/feed/",
            description = "Marketing news and insights from Web Strategist",
            logoUrl = "https://www.google.com/s2/favicons?domain=web-strategist.com&sz=64",
        ),
        SuggestedFeed(
            name = "Webbiquity",
            url = "http://feeds.feedburner.com/Webbiquity",
            description = "Marketing news and insights from Webbiquity",
            logoUrl = "https://www.google.com/s2/favicons?domain=webbiquity.com&sz=64",
        ),
        SuggestedFeed(
            name = "Wordstream",
            url = "http://feeds.feedburner.com/WordStreamBlog",
            description = "Marketing news and insights from Wordstream",
            logoUrl = "https://www.google.com/s2/favicons?domain=wordstream.com&sz=64",
        ),
        SuggestedFeed(
            name = "Yoast",
            url = "https://yoast.com/feed/",
            description = "Marketing news and insights from Yoast",
            logoUrl = "https://www.google.com/s2/favicons?domain=yoast.com&sz=64",
        ),
    ),
)
