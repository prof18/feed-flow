@file:Suppress("MagicNumber")

package com.prof18.feedflow.shared.e2e

import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.BackgroundSyncRestrictions
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod

class E2eSeedRunner internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val settingsRepository: SettingsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val accountsRepository: AccountsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val feedStateRepository: FeedStateRepository,
) {
    suspend fun reset() {
        databaseHelper.deleteAllE2eData()
        feedSyncRepository.deleteAll()
        accountsRepository.clearAllAccounts()
        feedItemContentFileHandler.clearAllContent()
        applyBaseSettings()
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
    }

    suspend fun seed(profile: E2eSeedProfile) {
        applyBaseSettings()
        if (profile == E2eSeedProfile.EMPTY) {
            feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
            return
        }

        seedContentRichData()
        applyProfileSettings(profile)
        feedStateRepository.updateFeedFilter(FeedFilter.Timeline)
    }

    suspend fun resetAndSeed(profile: E2eSeedProfile) {
        reset()
        seed(profile)
    }

    suspend fun run(action: String, profileName: String?) {
        val profile = E2eSeedProfile.fromQueryValue(profileName)
            ?: E2eSeedProfile.CONTENT_RICH
        when (action) {
            ACTION_RESET -> reset()
            ACTION_SEED -> seed(profile)
            ACTION_RESET_AND_SEED -> resetAndSeed(profile)
            else -> error("Unsupported E2E seed action: $action")
        }
    }

    private fun applyBaseSettings() {
        settingsRepository.clearFavouriteBrowserId()
        settingsRepository.setMarkFeedAsReadWhenScrolling(true)
        settingsRepository.setShowReadArticlesTimeline(false)
        settingsRepository.setHideReadItems(false)
        settingsRepository.setUseReaderMode(true)
        settingsRepository.setSaveItemContentOnOpen(false)
        settingsRepository.setPrefetchArticleContent(false)
        settingsRepository.setIsSyncUploadRequired(false)
        settingsRepository.setReaderModeFontSize(SettingsRepository.DEFAULT_READER_MODE_FONT_SIZE)
        settingsRepository.setAutoDeletePeriod(AutoDeletePeriod.DISABLED)
        settingsRepository.setCrashReportingEnabled(false)
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)
        settingsRepository.setBackgroundSyncRestrictions(
            BackgroundSyncRestrictions(
                syncOnlyOnWifi = false,
                syncOnlyWhenCharging = false,
            ),
        )
        settingsRepository.setThemeMode(ThemeMode.SYSTEM)
        settingsRepository.setReduceMotionEnabled(true)
        settingsRepository.setRefreshFeedsOnLaunch(false)
        settingsRepository.setShowRssParsingErrors(false)
        settingsRepository.setNotificationMode(NotificationMode.FEED_SOURCE)

        feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.NEWEST_FIRST)
        feedAppearanceSettingsRepository.setRemoveTitleFromDescription(false)
        feedAppearanceSettingsRepository.setHideDescription(false)
        feedAppearanceSettingsRepository.setFeedListFontScaleFactor(0)
        feedAppearanceSettingsRepository.setHideImages(false)
        feedAppearanceSettingsRepository.setHideDate(false)
        feedAppearanceSettingsRepository.setSwipeAction(SwipeDirection.LEFT, SwipeActionType.NONE)
        feedAppearanceSettingsRepository.setSwipeAction(SwipeDirection.RIGHT, SwipeActionType.NONE)
        feedAppearanceSettingsRepository.setFeedLayout(FeedLayout.LIST)
        feedAppearanceSettingsRepository.setHideUnreadDot(false)
        feedAppearanceSettingsRepository.setHideUnreadCount(false)
        feedAppearanceSettingsRepository.setHideFeedSource(false)
        feedAppearanceSettingsRepository.setDescriptionLineLimit(DescriptionLineLimit.THREE)
    }

    private suspend fun seedContentRichData() {
        databaseHelper.addBlockedWord(BLOCKED_WORD)
        databaseHelper.insertCategories(seedCategories)
        databaseHelper.insertFeedSource(seedParsedFeedSources)
        seedFeedSourcePreferences()
        databaseHelper.insertFeedItems(seedFeedItems, lastSyncTimestamp = SEED_NOW_MILLIS)
        databaseHelper.updateFeedSourceFetchFailed(
            feedSourceId = FETCH_FAILED_FEED_ID,
            fetchFailed = true,
        )
        seedReadAndBookmarkState()
        seedReaderContent()
    }

    private suspend fun seedFeedSourcePreferences() {
        seedFeedSources.forEach { feedSource ->
            databaseHelper.insertFeedSourcePreference(
                feedSourceId = feedSource.id,
                preference = feedSource.linkOpeningPreference,
                isHidden = feedSource.isHiddenFromTimeline,
                isPinned = feedSource.isPinned,
                isNotificationEnabled = feedSource.isNotificationEnabled,
            )
        }
    }

    private suspend fun seedReadAndBookmarkState() {
        val readItems = listOf(
            READ_ARTICLE_ID,
            BOOKMARKED_READ_ARTICLE_ID,
        )
        readItems.forEach { itemId ->
            databaseHelper.updateReadStatus(
                feedItemId = FeedItemId(itemId),
                isRead = true,
            )
        }

        val bookmarkedItems = listOf(
            BOOKMARKED_UNREAD_ARTICLE_ID,
            BOOKMARKED_READ_ARTICLE_ID,
        )
        bookmarkedItems.forEach { itemId ->
            databaseHelper.updateBookmarkStatus(
                feedItemId = FeedItemId(itemId),
                isBookmarked = true,
            )
        }
    }

    private suspend fun seedReaderContent() {
        feedItemContentFileHandler.saveFeedItemContentToFile(
            feedItemId = READER_SUCCESS_ARTICLE_ID,
            content = READER_SUCCESS_HTML,
        )
        databaseHelper.updateContentFetchedStatus(
            feedItemId = READER_SUCCESS_ARTICLE_ID,
            fetched = true,
        )

        feedItemContentFileHandler.saveFeedItemContentToFile(
            feedItemId = READER_FALLBACK_ARTICLE_ID,
            content = READER_FALLBACK_HTML,
        )
        databaseHelper.updateContentFetchedStatus(
            feedItemId = READER_FALLBACK_ARTICLE_ID,
            fetched = true,
        )
    }

    private suspend fun applyProfileSettings(profile: E2eSeedProfile) {
        when (profile) {
            E2eSeedProfile.EMPTY,
            E2eSeedProfile.CONTENT_RICH,
            E2eSeedProfile.SYNC_LINKED_MOCK,
            -> Unit

            E2eSeedProfile.CARD_LAYOUT -> applyCardLayoutSettings()
            E2eSeedProfile.COMPACT_LIST -> applyCompactListSettings()
            E2eSeedProfile.READER_MODE -> applyReaderModeSettings()
            E2eSeedProfile.EXTERNAL_BROWSER -> applyExternalBrowserSettings()
            E2eSeedProfile.READ_BEHAVIOR -> applyReadBehaviorSettings()
            E2eSeedProfile.OLDEST_FIRST -> feedAppearanceSettingsRepository.setFeedOrder(FeedOrder.OLDEST_FIRST)
            E2eSeedProfile.NOTIFICATIONS -> applyNotificationSettings()
            E2eSeedProfile.ANDROID_WIDGET -> applyCardLayoutSettings()
        }
    }

    private fun applyCardLayoutSettings() {
        feedAppearanceSettingsRepository.setFeedLayout(FeedLayout.CARD)
        feedAppearanceSettingsRepository.setHideImages(false)
        feedAppearanceSettingsRepository.setHideDate(false)
        feedAppearanceSettingsRepository.setHideDescription(false)
        feedAppearanceSettingsRepository.setDescriptionLineLimit(DescriptionLineLimit.THREE)
    }

    private fun applyCompactListSettings() {
        feedAppearanceSettingsRepository.setFeedLayout(FeedLayout.LIST)
        feedAppearanceSettingsRepository.setHideImages(true)
        feedAppearanceSettingsRepository.setHideDate(true)
        feedAppearanceSettingsRepository.setHideFeedSource(true)
        feedAppearanceSettingsRepository.setHideUnreadDot(true)
        feedAppearanceSettingsRepository.setHideDescription(true)
        feedAppearanceSettingsRepository.setRemoveTitleFromDescription(true)
    }

    private fun applyReaderModeSettings() {
        settingsRepository.setUseReaderMode(true)
        settingsRepository.setSaveItemContentOnOpen(true)
        settingsRepository.setPrefetchArticleContent(false)
        settingsRepository.setReaderModeFontSize(READER_MODE_PROFILE_FONT_SIZE)
    }

    private suspend fun applyExternalBrowserSettings() {
        settingsRepository.setUseReaderMode(false)
        val preferencesByFeedId = mapOf(
            ANDROID_WEEKLY_FEED_ID to LinkOpeningPreference.DEFAULT,
            SWIFT_WEEKLY_FEED_ID to LinkOpeningPreference.READER_MODE,
            WORLD_NEWS_FEED_ID to LinkOpeningPreference.INTERNAL_BROWSER,
            UNCATEGORIZED_FEED_ID to LinkOpeningPreference.PREFERRED_BROWSER,
        )
        preferencesByFeedId.forEach { (feedId, preference) ->
            val feedSource = seedFeedSources.first { it.id == feedId }
            databaseHelper.insertFeedSourcePreference(
                feedSourceId = feedId,
                preference = preference,
                isHidden = feedSource.isHiddenFromTimeline,
                isPinned = feedSource.isPinned,
                isNotificationEnabled = feedSource.isNotificationEnabled,
            )
        }
    }

    private fun applyReadBehaviorSettings() {
        settingsRepository.setShowReadArticlesTimeline(true)
        settingsRepository.setMarkFeedAsReadWhenScrolling(true)
        settingsRepository.setHideReadItems(true)
    }

    private suspend fun applyNotificationSettings() {
        settingsRepository.setNotificationMode(NotificationMode.FEED_SOURCE)
        settingsRepository.setSyncPeriod(SyncPeriod.THIRTY_MINUTES)
        val enabledFeedIds = setOf(
            ANDROID_WEEKLY_FEED_ID,
            WORLD_NEWS_FEED_ID,
            PINNED_FEED_ID,
        )
        seedFeedSources.forEach { feedSource ->
            databaseHelper.updateNotificationEnabledStatus(
                feedSourceId = feedSource.id,
                enabled = feedSource.id in enabledFeedIds,
            )
        }
    }

    companion object {
        const val ACTION_RESET = "reset"
        const val ACTION_SEED = "seed"
        const val ACTION_RESET_AND_SEED = "reset-and-seed"

        const val BLOCKED_WORD = "blockedword"
        const val READER_SUCCESS_ARTICLE_ID = "e2e-article-reader-success"
        const val READER_FALLBACK_ARTICLE_ID = "e2e-article-reader-fallback"

        private const val TECHNOLOGY_CATEGORY_ID = "e2e-category-technology"
        private const val NEWS_CATEGORY_ID = "e2e-category-news"
        private const val EMPTY_CATEGORY_ID = "e2e-category-empty"

        private const val ANDROID_WEEKLY_FEED_ID = "e2e-feed-android-weekly"
        private const val SWIFT_WEEKLY_FEED_ID = "e2e-feed-swift-weekly"
        private const val WORLD_NEWS_FEED_ID = "e2e-feed-world-news"
        private const val UNCATEGORIZED_FEED_ID = "e2e-feed-uncategorized"
        private const val FETCH_FAILED_FEED_ID = "e2e-feed-fetch-failed"
        private const val HIDDEN_FEED_ID = "e2e-feed-hidden"
        private const val PINNED_FEED_ID = "e2e-feed-pinned"

        private const val NEWEST_ARTICLE_ID = "e2e-article-newest-unread"
        private const val READ_ARTICLE_ID = "e2e-article-read"
        private const val BOOKMARKED_UNREAD_ARTICLE_ID = "e2e-article-bookmarked-unread"
        private const val BOOKMARKED_READ_ARTICLE_ID = "e2e-article-bookmarked-read"
        private const val IMAGE_ARTICLE_ID = "e2e-article-image"
        private const val NO_IMAGE_ARTICLE_ID = "e2e-article-no-image"
        private const val COMMENTS_ARTICLE_ID = "e2e-article-comments"
        private const val OLD_ARTICLE_ID = "e2e-article-old"
        private const val DUPLICATED_TITLE_ARTICLE_ID = "e2e-article-duplicate-title"
        private const val BLOCKED_ARTICLE_ID = "e2e-article-blocked"
        private const val HIDDEN_ARTICLE_ID = "e2e-article-hidden"
        private const val PINNED_ARTICLE_ID = "e2e-article-pinned"

        private const val SEED_NOW_MILLIS = 1_765_152_000_000L
        private const val ONE_HOUR_MILLIS = 3_600_000L
        private const val ONE_DAY_MILLIS = 86_400_000L
        private const val OLD_ARTICLE_OFFSET_MILLIS = ONE_DAY_MILLIS * 90
        private const val READER_MODE_PROFILE_FONT_SIZE = 20

        private val technologyCategory = FeedSourceCategory(
            id = TECHNOLOGY_CATEGORY_ID,
            title = "Technology",
        )
        private val newsCategory = FeedSourceCategory(
            id = NEWS_CATEGORY_ID,
            title = "News",
        )
        private val emptyCategory = FeedSourceCategory(
            id = EMPTY_CATEGORY_ID,
            title = "Empty Category",
        )

        private val seedCategories = listOf(
            technologyCategory,
            newsCategory,
            emptyCategory,
        )

        private val seedFeedSources = listOf(
            feedSource(
                id = ANDROID_WEEKLY_FEED_ID,
                url = "https://www.androidcentral.com/feed",
                title = "Android Central RSS Feed",
                category = technologyCategory,
                logoUrl = "https://cdn.mos.cms.futurecdn.net/flexiimages/ojylbffmdc1632303233.png",
                websiteUrl = "https://www.androidcentral.com/feeds.xml",
            ),
            feedSource(
                id = SWIFT_WEEKLY_FEED_ID,
                url = "https://www.androidpolice.com/feed/",
                title = "Android Police",
                category = technologyCategory,
                logoUrl = "https://www.androidpolice.com/public/build/images/favicon-48x48.png",
                websiteUrl = "https://www.androidpolice.com",
            ),
            feedSource(
                id = WORLD_NEWS_FEED_ID,
                url = "https://feeds.bloomberg.com/technology/news.rss",
                title = "Bloomberg Technology",
                category = newsCategory,
                logoUrl = "https://www.bloomberg.com/feeds/static/images/bloomberg_logo_blue.png",
                websiteUrl = "https://bloomberg.com/technology/",
            ),
            feedSource(
                id = UNCATEGORIZED_FEED_ID,
                url = "https://feeds.feedburner.com/hd-blog",
                title = "HDblog.it",
                category = null,
                logoUrl = "https://www.hdblog.it/new_files/templates/theme_darklight/img/logos_wt/logohd.png",
                websiteUrl = "https://www.hdblog.it",
            ),
            feedSource(
                id = FETCH_FAILED_FEED_ID,
                url = "https://www.veneziatoday.it/rss",
                title = "VeneziaToday",
                category = newsCategory,
                fetchFailed = true,
                logoUrl = "https://citynews-veneziatoday.stgy.ovh/images/v2015/brand/favicon.png",
                websiteUrl = "https://www.veneziatoday.it/",
            ),
            feedSource(
                id = HIDDEN_FEED_ID,
                url = "https://feeds.bloomberg.com/politics/news.rss",
                title = "Bloomberg Politics",
                category = technologyCategory,
                isHiddenFromTimeline = true,
                logoUrl = "https://www.bloomberg.com/feeds/static/images/bloomberg_logo_blue.png",
                websiteUrl = "https://bloomberg.com/politics/",
            ),
            feedSource(
                id = PINNED_FEED_ID,
                url = "https://feeds.arstechnica.com/arstechnica/index",
                title = "Ars Technica",
                category = null,
                isPinned = true,
                logoUrl = "https://cdn.arstechnica.net/wp-content/uploads/2016/10/cropped-ars-logo-512_480-60x60.png",
                websiteUrl = "https://arstechnica.com",
            ),
        )

        private val seedParsedFeedSources = seedFeedSources.map { feedSource ->
            ParsedFeedSource(
                id = feedSource.id,
                url = feedSource.url,
                title = feedSource.title,
                category = feedSource.category,
                logoUrl = feedSource.logoUrl,
                websiteUrl = feedSource.websiteUrl,
            )
        }

        private val androidWeekly = seedFeedSources.first { it.id == ANDROID_WEEKLY_FEED_ID }
        private val swiftWeekly = seedFeedSources.first { it.id == SWIFT_WEEKLY_FEED_ID }
        private val worldNews = seedFeedSources.first { it.id == WORLD_NEWS_FEED_ID }
        private val uncategorizedFeed = seedFeedSources.first { it.id == UNCATEGORIZED_FEED_ID }
        private val fetchFailedFeed = seedFeedSources.first { it.id == FETCH_FAILED_FEED_ID }
        private val hiddenFeed = seedFeedSources.first { it.id == HIDDEN_FEED_ID }
        private val pinnedFeed = seedFeedSources.first { it.id == PINNED_FEED_ID }

        private val seedFeedItems = listOf(
            feedItem(
                id = NEWEST_ARTICLE_ID,
                title = "E2E Newest Unread Article: Motorola Razr deals",
                subtitle = "A realistic seeded item using Android Central image metadata",
                feedSource = androidWeekly,
                url = "https://www.androidcentral.com/phones/motorola/" +
                    "best-motorola-razr-2026-deals-of-the-month",
                imageUrl = "https://cdn.mos.cms.futurecdn.net/kDPsA7KqMQchuAKRoTZozb-1280-80.jpg",
                pubDateMillis = SEED_NOW_MILLIS,
            ),
            feedItem(
                id = READ_ARTICLE_ID,
                title = "E2E Read Article: Gemini in Android Auto",
                subtitle = "Already read seeded item with Android Police source metadata",
                feedSource = androidWeekly,
                url = "https://www.androidpolice.com/tips-to-get-best-out-of-gemini-in-android-auto/",
                imageUrl = "https://static0.anpoimages.com/wordpress/wp-content/uploads/2026/05/" +
                    "gemini-in-android-auto.png",
                pubDateMillis = SEED_NOW_MILLIS - ONE_HOUR_MILLIS,
            ),
            feedItem(
                id = BOOKMARKED_UNREAD_ARTICLE_ID,
                title = "E2E Bookmarked Unread Article: Pixel battery debate",
                subtitle = "Unread bookmark seeded item with real image metadata",
                feedSource = swiftWeekly,
                url = "https://www.androidpolice.com/" +
                    "pixel-phones-getting-better-why-battery-complaints-still-follow/",
                imageUrl = "https://static0.anpoimages.com/wordpress/wp-content/uploads/2026/05/" +
                    "two-google-pixel-smartphones-with-a-battery-warning-symbol.png",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 2),
            ),
            feedItem(
                id = BOOKMARKED_READ_ARTICLE_ID,
                title = "E2E Bookmarked Read Article: Quantum computing deal",
                subtitle = "Read bookmark seeded item with comments URL",
                feedSource = swiftWeekly,
                url = "https://arstechnica.com/tech-policy/2026/05/" +
                    "uss-big-bet-on-quantum-computing-may-not-be-entirely-legal/",
                imageUrl = "https://cdn.arstechnica.net/wp-content/uploads/2026/05/image-2-1152x648.jpeg",
                commentsUrl = "https://arstechnica.com/tech-policy/2026/05/" +
                    "uss-big-bet-on-quantum-computing-may-not-be-entirely-legal/#comments",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 3),
            ),
            feedItem(
                id = IMAGE_ARTICLE_ID,
                title = "E2E Article With Image: Delivery Hero stake",
                subtitle = "Seeded item with Bloomberg image metadata",
                feedSource = worldNews,
                url = "https://www.bloomberg.com/news/articles/2026-05-25/" +
                    "prosus-asks-eu-to-end-forced-sale-of-delivery-hero-stake",
                imageUrl = "https://assets.bwbx.io/images/users/iqjWHBFdfxIU/iAAytUdL0AHw/v0/1200x-1.jpg",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 4),
            ),
            feedItem(
                id = NO_IMAGE_ARTICLE_ID,
                title = "E2E Article Without Image: DeepSeek pricing",
                subtitle = "Seeded item without media",
                feedSource = uncategorizedFeed,
                url = "https://www.hdblog.it/tecnologia/articoli/n659380/deepseek-sconto-v4-permanente/",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 5),
            ),
            feedItem(
                id = COMMENTS_ARTICLE_ID,
                title = "E2E Article With Comments: Pixel Glow",
                subtitle = "Seeded item with an HDblog comments URL",
                feedSource = worldNews,
                url = "https://www.hdblog.it/smartphone/articoli/n659372/pixel-11-google-glow-led/",
                imageUrl = "https://hd2.tudocdn.net/1344815?w=180&h=180&ext=file.jpg",
                commentsUrl = "https://www.hdblog.it/smartphone/articoli/n659372/pixel-11-google-glow-led/#comments",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 6),
            ),
            feedItem(
                id = OLD_ARTICLE_ID,
                title = "E2E Old Article Eligible For Delete: Venezia council",
                subtitle = "Seeded item older than cleanup thresholds",
                feedSource = fetchFailedFeed,
                url = "https://www.veneziatoday.it/politica/elezioni/comunali-2026/" +
                    "composizione-consiglio-comunale.html",
                imageUrl = "https://citynews-veneziatoday.stgy.ovh/~media/original-hi/" +
                    "54141317733786/consiglio-comunale-ca-loredan-ca-farsetti-2.jpg",
                pubDateMillis = SEED_NOW_MILLIS - OLD_ARTICLE_OFFSET_MILLIS,
            ),
            feedItem(
                id = DUPLICATED_TITLE_ARTICLE_ID,
                title = "E2E Duplicated Title",
                subtitle = "E2E Duplicated Title",
                feedSource = androidWeekly,
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 7),
            ),
            feedItem(
                id = BLOCKED_ARTICLE_ID,
                title = "E2E blockedword Article",
                subtitle = "This item should be hidden by the seeded blocked word",
                feedSource = worldNews,
                url = "https://www.hdblog.it/sicurezza/articoli/n659388/" +
                    "claude-mythos-preview-vulnerabilita-primo-mese/",
                imageUrl = "https://hd2.tudocdn.net/1341310?w=180&h=180&ext=file.jpg",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 8),
            ),
            feedItem(
                id = READER_SUCCESS_ARTICLE_ID,
                title = "E2E Reader Mode Success Article: Interstitium",
                subtitle = "Seeded item with cached reader content",
                feedSource = swiftWeekly,
                url = "https://www.ilpost.it/2026/05/25/interstitium/",
                imageUrl = "https://static-prod.cdnilpost.com/wp-content/uploads/2026/05/25/" +
                    "680x453/1779707841-GettyImages-113189195.jpg",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 9),
            ),
            feedItem(
                id = READER_FALLBACK_ARTICLE_ID,
                title = "E2E Reader Mode Fallback Article: The Witcher 3",
                subtitle = "Seeded item for fallback reader behavior",
                feedSource = uncategorizedFeed,
                url = "https://arstechnica.com/gaming/2026/05/" +
                    "the-witcher-3-is-a-good-game-but-that-doesnt-mean-you-have-to-like-it/",
                imageUrl = "https://cdn.arstechnica.net/wp-content/uploads/2026/05/" +
                    "The-Witcher-3-hero-image-1152x648-1779476972.jpg",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 10),
            ),
            feedItem(
                id = HIDDEN_ARTICLE_ID,
                title = "E2E Hidden Feed Article",
                subtitle = "This item belongs to a hidden feed",
                feedSource = hiddenFeed,
                url = "https://www.bloomberg.com/news/newsletters/2026-05-25/" +
                    "donald-trump-pushes-back-against-republican-critics-of-emerging-iran-deal",
                imageUrl = "https://assets.bwbx.io/images/users/iqjWHBFdfxIU/iOiXl22S1Ky4/v8/1200x-1.jpg",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 11),
            ),
            feedItem(
                id = PINNED_ARTICLE_ID,
                title = "E2E Pinned Feed Article: Eufy C15",
                subtitle = "This item belongs to a pinned feed",
                feedSource = pinnedFeed,
                url = "https://www.hdblog.it/home/articoli/n659381/" +
                    "anker-eufy-c15-tagliaerba-robot-economico/",
                imageUrl = "https://hd2.tudocdn.net/1344809?w=180&h=180&ext=file.jpg",
                pubDateMillis = SEED_NOW_MILLIS - (ONE_HOUR_MILLIS * 12),
            ),
        )

        private const val READER_SUCCESS_HTML = """
            <article>
              <h1>E2E Reader Mode Success Article</h1>
              <p>E2E cached reader content loaded from the seed fixture.</p>
            </article>
        """

        private const val READER_FALLBACK_HTML = """
            <article>
              <h1>E2E Reader Mode Fallback Article</h1>
              <p>E2E fallback reader content loaded from the seed fixture.</p>
            </article>
        """

        private fun feedSource(
            id: String,
            url: String,
            title: String,
            category: FeedSourceCategory?,
            fetchFailed: Boolean = false,
            isHiddenFromTimeline: Boolean = false,
            isPinned: Boolean = false,
            logoUrl: String? = null,
            websiteUrl: String = url.removeSuffix(".xml"),
        ): FeedSource =
            FeedSource(
                id = id,
                url = url,
                title = title,
                category = category,
                lastSyncTimestamp = SEED_NOW_MILLIS,
                logoUrl = logoUrl,
                websiteUrl = websiteUrl,
                fetchFailed = fetchFailed,
                linkOpeningPreference = LinkOpeningPreference.DEFAULT,
                isHiddenFromTimeline = isHiddenFromTimeline,
                isPinned = isPinned,
                isNotificationEnabled = false,
            )

        private fun feedItem(
            id: String,
            title: String,
            subtitle: String,
            feedSource: FeedSource,
            pubDateMillis: Long,
            url: String = "https://e2e.feedflow.local/articles/$id",
            imageUrl: String? = null,
            commentsUrl: String? = null,
        ): FeedItem =
            FeedItem(
                id = id,
                url = url,
                title = title,
                subtitle = subtitle,
                content = null,
                imageUrl = imageUrl,
                feedSource = feedSource,
                pubDateMillis = pubDateMillis,
                isRead = false,
                dateString = null,
                commentsUrl = commentsUrl,
                isBookmarked = false,
            )
    }
}
