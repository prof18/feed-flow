package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory

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
                    ),
                    SuggestedFeed(
                        name = "The Verge",
                        url = "https://www.theverge.com/rss/index.xml",
                        description = "Technology, science, art, and culture",
                    ),
                    SuggestedFeed(
                        name = "Hacker News",
                        url = "https://hnrss.org/frontpage",
                        description = "News for hackers",
                    ),
                    SuggestedFeed(
                        name = "Ars Technica",
                        url = "https://feeds.arstechnica.com/arstechnica/index",
                        description = "Technology news and analysis",
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
                    ),
                    SuggestedFeed(
                        name = "Reuters",
                        url = "https://www.reutersagency.com/feed/",
                        description = "International news and business",
                    ),
                    SuggestedFeed(
                        name = "The Guardian",
                        url = "https://www.theguardian.com/world/rss",
                        description = "World news and opinion",
                    ),
                    SuggestedFeed(
                        name = "CNN Top Stories",
                        url = "http://rss.cnn.com/rss/cnn_topstories.rss",
                        description = "Latest news headlines",
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
                    ),
                    SuggestedFeed(
                        name = "JavaScript Weekly",
                        url = "https://javascriptweekly.com/rss",
                        description = "A newsletter of JavaScript articles, news and cool projects",
                    ),
                    SuggestedFeed(
                        name = "Kotlin Blog",
                        url = "https://blog.jetbrains.com/kotlin/feed/",
                        description = "Official Kotlin programming language blog",
                    ),
                    SuggestedFeed(
                        name = "Android Developers Blog",
                        url = "https://android-developers.googleblog.com/feeds/posts/default",
                        description = "The latest Android and Google Play news",
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
                    ),
                    SuggestedFeed(
                        name = "Science Daily",
                        url = "https://www.sciencedaily.com/rss/all.xml",
                        description = "Latest science news",
                    ),
                    SuggestedFeed(
                        name = "Nature",
                        url = "https://www.nature.com/nature.rss",
                        description = "International journal of science",
                    ),
                    SuggestedFeed(
                        name = "Scientific American",
                        url = "http://rss.sciam.com/ScientificAmerican-Global",
                        description = "Science news and technology updates",
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
                    ),
                    SuggestedFeed(
                        name = "Designer News",
                        url = "https://www.designernews.co/?format=rss",
                        description = "Design community news",
                    ),
                    SuggestedFeed(
                        name = "Dribbble",
                        url = "https://dribbble.com/shots/popular.rss",
                        description = "Design inspiration",
                    ),
                    SuggestedFeed(
                        name = "Awwwards",
                        url = "https://www.awwwards.com/blog/feed/",
                        description = "Web design trends and inspiration",
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
                    ),
                    SuggestedFeed(
                        name = "Forbes",
                        url = "https://www.forbes.com/business/feed/",
                        description = "Business news and financial updates",
                    ),
                    SuggestedFeed(
                        name = "Entrepreneur",
                        url = "https://www.entrepreneur.com/latest.rss",
                        description = "Entrepreneurship and startup news",
                    ),
                    SuggestedFeed(
                        name = "Bloomberg",
                        url = "https://www.bloomberg.com/feed/podcast/what-goes-up.xml",
                        description = "Business and financial news",
                    ),
                ),
            ),
        )
    }
}
