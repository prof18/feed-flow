package com.prof18.feedflow.shared.domain.feed.suggestions.bycategory

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

internal val lifestyleFeeds = SuggestedFeedCategory(
    id = "lifestyle",
    name = "Lifestyle",
    icon = "🏡",
    feeds = listOf(
        SuggestedFeed(
            name = "Atlas Obscura",
            url = "https://www.atlasobscura.com/feeds/latest",
            description = "Unusual destinations and stories",
            logoUrl = "https://www.google.com/s2/favicons?domain=atlasobscura.com&sz=64",
        ),
        SuggestedFeed(
            name = "Condé Nast Traveler",
            url = "https://www.cntraveler.com/feed/rss",
            description = "Luxury travel and destinations",
            logoUrl = "https://www.google.com/s2/favicons?domain=cntraveler.com&sz=64",
        ),
        SuggestedFeed(
            name = "Dezeen",
            url = "https://www.dezeen.com/feed/",
            description = "Architecture and design news",
            logoUrl = "https://www.google.com/s2/favicons?domain=dezeen.com&sz=64",
        ),
        SuggestedFeed(
            name = "Esquire Style",
            url = "https://www.esquire.com/rss/style.xml",
            description = "Men's style and grooming",
            logoUrl = "https://www.google.com/s2/favicons?domain=esquire.com&sz=64",
        ),
        SuggestedFeed(
            name = "Fashion Network",
            url = "https://ww.fashionnetwork.com/rss/feed/ww,1.xml",
            description = "Fashion industry business news",
            logoUrl = "https://www.google.com/s2/favicons?domain=fashionnetwork.com&sz=64",
        ),
        SuggestedFeed(
            name = "GQ",
            url = "https://www.gq.com/feed/rss",
            description = "Men's fashion and lifestyle",
            logoUrl = "https://www.google.com/s2/favicons?domain=gq.com&sz=64",
        ),
        SuggestedFeed(
            name = "Hypebeast Fashion",
            url = "https://hypebeast.com/fashion/feed",
            description = "Streetwear and fashion culture",
            logoUrl = "https://www.google.com/s2/favicons?domain=hypebeast.com&sz=64",
        ),
        SuggestedFeed(
            name = "Lifehacker",
            url = "https://lifehacker.com/rss",
            description = "Tips and tricks for getting things done",
            logoUrl = "https://www.google.com/s2/favicons?domain=lifehacker.com&sz=64",
        ),
        SuggestedFeed(
            name = "Nomadic Matt",
            url = "https://www.nomadicmatt.com/feed/",
            description = "Travel tips and budget advice",
            logoUrl = "https://www.google.com/s2/favicons?domain=nomadicmatt.com&sz=64",
        ),
        SuggestedFeed(
            name = "Smashing Magazine",
            url = "https://www.smashingmagazine.com/feed/",
            description = "Web design and development",
            logoUrl = "https://www.google.com/s2/favicons?domain=smashingmagazine.com&sz=64",
        ),
        SuggestedFeed(
            name = "The Points Guy",
            url = "https://thepointsguy.com/feed/",
            description = "Travel rewards and tips",
            logoUrl = "https://www.google.com/s2/favicons?domain=thepointsguy.com&sz=64",
        ),
        SuggestedFeed(
            name = "Vox",
            url = "https://www.vox.com/rss/index.xml",
            description = "Explanatory journalism and news",
            logoUrl = "https://www.google.com/s2/favicons?domain=vox.com&sz=64",
        ),
    ),
)
