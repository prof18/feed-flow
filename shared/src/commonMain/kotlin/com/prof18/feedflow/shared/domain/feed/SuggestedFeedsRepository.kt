package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

// TODO: move outside the repository to a file without class
//  make a single file by category
class SuggestedFeedsRepository {

    fun getSuggestedFeeds(): List<SuggestedFeedCategory> {
        return listOf(
            SuggestedFeedCategory(
                id = "tech",
                name = "Technology",
                icon = "💻",
                feeds = listOf(
                    SuggestedFeed(
                        name = "TechCrunch",
                        url = "https://techcrunch.com/feed/",
                        description = "The latest technology news and information on startups",
                        logoUrl = "https://www.google.com/s2/favicons?domain=techcrunch.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "The Verge",
                        url = "https://www.theverge.com/rss/index.xml",
                        description = "Technology, science, art, and culture",
                        logoUrl = "https://www.google.com/s2/favicons?domain=theverge.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Hacker News",
                        url = "https://hnrss.org/frontpage",
                        description = "News for hackers",
                        logoUrl = "https://www.google.com/s2/favicons?domain=news.ycombinator.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Ars Technica",
                        url = "https://feeds.arstechnica.com/arstechnica/index",
                        description = "Technology news and analysis",
                        logoUrl = "https://www.google.com/s2/favicons?domain=arstechnica.com&sz=64",
                    ),
                ),
            ),
            SuggestedFeedCategory(
                id = "news",
                name = "News",
                icon = "📰",
                feeds = listOf(
                    SuggestedFeed(
                        name = "BBC News",
                        url = "http://feeds.bbci.co.uk/news/rss.xml",
                        description = "Breaking news from around the world",
                        logoUrl = "https://www.google.com/s2/favicons?domain=bbc.co.uk&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Reuters",
                        url = "https://www.reutersagency.com/feed/",
                        description = "International news and business",
                        logoUrl = "https://www.google.com/s2/favicons?domain=reuters.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "The Guardian",
                        url = "https://www.theguardian.com/world/rss",
                        description = "World news and opinion",
                        logoUrl = "https://www.google.com/s2/favicons?domain=theguardian.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "CNN Top Stories",
                        url = "http://rss.cnn.com/rss/cnn_topstories.rss",
                        description = "Latest news headlines",
                        logoUrl = "https://www.google.com/s2/favicons?domain=cnn.com&sz=64",
                    ),
                ),
            ),
            SuggestedFeedCategory(
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
            ),
            SuggestedFeedCategory(
                id = "science",
                name = "Science",
                icon = "🔬",
                feeds = listOf(
                    SuggestedFeed(
                        name = "NASA Breaking News",
                        url = "https://www.nasa.gov/rss/dyn/breaking_news.rss",
                        description = "Latest NASA news",
                        logoUrl = "https://www.google.com/s2/favicons?domain=nasa.gov&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Science Daily",
                        url = "https://www.sciencedaily.com/rss/all.xml",
                        description = "Latest science news",
                        logoUrl = "https://www.google.com/s2/favicons?domain=sciencedaily.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Nature",
                        url = "https://www.nature.com/nature.rss",
                        description = "International journal of science",
                        logoUrl = "https://www.google.com/s2/favicons?domain=nature.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Scientific American",
                        url = "http://rss.sciam.com/ScientificAmerican-Global",
                        description = "Science news and technology updates",
                        logoUrl = "https://www.google.com/s2/favicons?domain=scientificamerican.com&sz=64",
                    ),
                ),
            ),
            SuggestedFeedCategory(
                id = "design",
                name = "Design",
                icon = "🎨",
                feeds = listOf(
                    SuggestedFeed(
                        name = "Smashing Magazine",
                        url = "https://www.smashingmagazine.com/feed/",
                        description = "Web design and development",
                        logoUrl = "https://www.google.com/s2/favicons?domain=smashingmagazine.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Designer News",
                        url = "https://www.designernews.co/?format=rss",
                        description = "Design community news",
                        logoUrl = "https://www.google.com/s2/favicons?domain=designernews.co&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Dribbble",
                        url = "https://dribbble.com/shots/popular.rss",
                        description = "Design inspiration",
                        logoUrl = "https://www.google.com/s2/favicons?domain=dribbble.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Awwwards",
                        url = "https://www.awwwards.com/blog/feed/",
                        description = "Web design trends and inspiration",
                        logoUrl = "https://www.google.com/s2/favicons?domain=awwwards.com&sz=64",
                    ),
                ),
            ),
            SuggestedFeedCategory(
                id = "business",
                name = "Business",
                icon = "💼",
                feeds = listOf(
                    SuggestedFeed(
                        name = "Harvard Business Review",
                        url = "https://feeds.hbr.org/harvardbusiness",
                        description = "Management and leadership insights",
                        logoUrl = "https://www.google.com/s2/favicons?domain=hbr.org&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Forbes",
                        url = "https://www.forbes.com/business/feed/",
                        description = "Business news and financial updates",
                        logoUrl = "https://www.google.com/s2/favicons?domain=forbes.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Entrepreneur",
                        url = "https://www.entrepreneur.com/latest.rss",
                        description = "Entrepreneurship and startup news",
                        logoUrl = "https://www.google.com/s2/favicons?domain=entrepreneur.com&sz=64",
                    ),
                    SuggestedFeed(
                        name = "Bloomberg",
                        url = "https://www.bloomberg.com/feed/podcast/what-goes-up.xml",
                        description = "Business and financial news",
                        logoUrl = "https://www.google.com/s2/favicons?domain=bloomberg.com&sz=64",
                    ),
                ),
            ),
        )
    }
}
