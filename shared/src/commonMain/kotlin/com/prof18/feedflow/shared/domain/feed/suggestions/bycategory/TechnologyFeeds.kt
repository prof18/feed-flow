package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val technologyFeeds = SuggestedFeedCategory(
    id = "technology",
    name = "Technology",
    icon = "💻",
    feeds = listOf(
        SuggestedFeed(
            name = "404 Media",
            url = "https://www.404media.co/rss/",
            description = "Independent technology journalism",
            logoUrl = "https://www.google.com/s2/favicons?domain=404media.co&sz=64",
        ),
        SuggestedFeed(
            name = "9to5Google",
            url = "https://9to5google.com/feed/",
            description = "Google and Android news",
            logoUrl = "https://www.google.com/s2/favicons?domain=9to5google.com&sz=64",
        ),
        SuggestedFeed(
            name = "9to5Linux",
            url = "https://9to5linux.com/feed",
            description = "Linux news, reviews, tutorials, and more",
            logoUrl = "https://www.google.com/s2/favicons?domain=9to5linux.com&sz=64",
        ),
        SuggestedFeed(
            name = "9to5Mac",
            url = "https://9to5mac.com/feed/",
            description = "Apple news and rumors",
            logoUrl = "https://www.google.com/s2/favicons?domain=9to5mac.com&sz=64",
        ),
        SuggestedFeed(
            name = "Android Authority",
            url = "https://www.androidauthority.com/feed/",
            description = "Android ecosystem news and device coverage",
            logoUrl = "https://www.google.com/s2/favicons?domain=androidauthority.com&sz=64",
        ),
        SuggestedFeed(
            name = "AppleInsider",
            url = "https://appleinsider.com/rss/news/",
            description = "Apple news and analysis",
            logoUrl = "https://www.google.com/s2/favicons?domain=appleinsider.com&sz=64",
        ),
        SuggestedFeed(
            name = "Ars Technica",
            url = "https://feeds.arstechnica.com/arstechnica/index",
            description = "Deep dives into software, hardware, and science",
            logoUrl = "https://www.google.com/s2/favicons?domain=arstechnica.com&sz=64",
        ),
        SuggestedFeed(
            name = "Daring Fireball",
            url = "https://daringfireball.net/feeds/articles",
            description = "Apple and tech commentary",
            logoUrl = "https://www.google.com/s2/favicons?domain=daringfireball.net&sz=64",
        ),
        SuggestedFeed(
            name = "Engadget",
            url = "https://www.engadget.com/rss-full.xml",
            description = "Technology news and reviews",
            logoUrl = "https://www.google.com/s2/favicons?domain=engadget.com&sz=64",
        ),
        SuggestedFeed(
            name = "Gizmodo",
            url = "https://gizmodo.com/rss",
            description = "Design, tech, and science news",
            logoUrl = "https://www.google.com/s2/favicons?domain=gizmodo.com&sz=64",
        ),
        SuggestedFeed(
            name = "GSM Arena",
            url = "https://www.gsmarena.com/rss-news-reviews.php3",
            description = "Technology news and updates from Gsmarena",
            logoUrl = "https://www.google.com/s2/favicons?domain=gsmarena.com&sz=64",
        ),
        SuggestedFeed(
            name = "Hacker News",
            url = "https://news.ycombinator.com/rss",
            description = "Top discussions from the tech and startup community",
            logoUrl = "https://www.google.com/s2/favicons?domain=ycombinator.com&sz=64",
        ),
        SuggestedFeed(
            name = "It's FOSS",
            url = "https://itsfoss.com/feed/",
            description = "Linux guides, news, and open source coverage",
            logoUrl = "https://www.google.com/s2/favicons?domain=itsfoss.com&sz=64",
        ),
        SuggestedFeed(
            name = "Last Week in AI",
            url = "https://lastweekin.ai/feed",
            description = "Weekly AI news digest",
            logoUrl = "https://www.google.com/s2/favicons?domain=lastweekin.ai&sz=64",
        ),
        SuggestedFeed(
            name = "Linux.com",
            url = "https://www.linux.com/feed/",
            description = "Open source news for professionals",
            logoUrl = "https://www.google.com/s2/favicons?domain=linux.com&sz=64",
        ),
        SuggestedFeed(
            name = "Linuxiac",
            url = "https://linuxiac.com/feed/",
            description = "Linux and open source news",
            logoUrl = "https://www.google.com/s2/favicons?domain=linuxiac.com&sz=64",
        ),
        SuggestedFeed(
            name = "MacRumors",
            url = "http://feeds.macrumors.com/MacRumors-Front",
            description = "Apple Mac news and rumors",
            logoUrl = "https://www.google.com/s2/favicons?domain=macrumors.com&sz=64",
        ),
        SuggestedFeed(
            name = "MacStories",
            url = "https://www.macstories.net/feed/",
            description = "Apple software and productivity coverage",
            logoUrl = "https://www.google.com/s2/favicons?domain=macstories.net&sz=64",
        ),
        SuggestedFeed(
            name = "Mashable",
            url = "http://feeds.mashable.com/mashable/tech",
            description = "Technology news and updates from Mashable",
            logoUrl = "https://www.google.com/s2/favicons?domain=mashable.com&sz=64",
        ),
        SuggestedFeed(
            name = "MIT News",
            url = "https://news.mit.edu/rss/feed",
            description = "Research and innovation from MIT",
            logoUrl = "https://www.google.com/s2/favicons?domain=mit.edu&sz=64",
        ),
        SuggestedFeed(
            name = "OMG! Ubuntu",
            url = "https://www.omgubuntu.co.uk/feed",
            description = "Ubuntu news, apps, tips, and desktop Linux coverage",
            logoUrl = "https://www.google.com/s2/favicons?domain=omgubuntu.co.uk&sz=64",
        ),
        SuggestedFeed(
            name = "Phonearena",
            url = "https://www.phonearena.com/feed/news",
            description = "Technology news and updates from Phonearena",
            logoUrl = "https://www.google.com/s2/favicons?domain=phonearena.com&sz=64",
        ),
        SuggestedFeed(
            name = "Phoronix",
            url = "https://www.phoronix.com/rss.php",
            description = "Linux hardware, benchmarking, and open source news",
            logoUrl = "https://www.google.com/s2/favicons?domain=phoronix.com&sz=64",
        ),
        SuggestedFeed(
            name = "Planet KDE",
            url = "https://planet.kde.org/index.xml",
            description = "Posts from KDE contributors and community blogs",
            logoUrl = "https://www.google.com/s2/favicons?domain=planet.kde.org&sz=64",
        ),
        SuggestedFeed(
            name = "Tech Emails",
            url = "https://www.techemails.com/feed",
            description = "Tech company emails and memos",
            logoUrl = "https://www.google.com/s2/favicons?domain=techemails.com&sz=64",
        ),
        SuggestedFeed(
            name = "TechCrunch",
            url = "https://techcrunch.com/feed/",
            description = "Startups, technology news, and product launches",
            logoUrl = "https://www.google.com/s2/favicons?domain=techcrunch.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Register",
            url = "https://www.theregister.com/headlines.rss",
            description = "Technology news and satire",
            logoUrl = "https://www.google.com/s2/favicons?domain=theregister.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Verge",
            url = "https://www.theverge.com/rss/index.xml",
            description = "Technology, science, and culture",
            logoUrl = "https://www.google.com/s2/favicons?domain=theverge.com&sz=64",
        ),
        SuggestedFeed(
            name = "Tonsky",
            url = "https://tonsky.me/atom.xml",
            description = "Technology news and updates from Tonsky",
            logoUrl = "https://www.google.com/s2/favicons?domain=tonsky.me&sz=64",
        ),
        SuggestedFeed(
            name = "Wired",
            url = "https://www.wired.com/feed/rss",
            description = "How technology is changing every aspect of our lives",
            logoUrl = "https://www.google.com/s2/favicons?domain=wired.com&sz=64",
        ),
        SuggestedFeed(
            name = "Zdnet",
            url = "https://www.zdnet.com/news/rss.xml",
            description = "Technology news and updates from Zdnet",
            logoUrl = "https://www.google.com/s2/favicons?domain=zdnet.com&sz=64",
        ),
    ),
)
