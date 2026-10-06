package com.prof18.feedflow.shared.di

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.getAppGroupDatabasePath
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.Cloud_pending_article_flag
import com.prof18.feedflow.db.Cloud_pending_feed_or_category_change
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.db.Feed_item_status
import com.prof18.feedflow.db.Feed_source
import com.prof18.feedflow.db.Feed_source_cache_info
import com.prof18.feedflow.db.Feed_source_category
import com.prof18.feedflow.db.Feed_source_preferences
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings

private fun createWidgetDatabase(appEnvironment: AppEnvironment): FeedFlowDB {
    val sqlDriver = NativeSqliteDriver(
        schema = FeedFlowDB.Schema,
        onConfiguration = { configuration ->
            configuration.copy(
                extendedConfig = configuration.extendedConfig.copy(
                    basePath = getAppGroupDatabasePath(),
                ),
            )
        },
        name = if (appEnvironment.isDebug()) {
            DatabaseHelper.APP_DATABASE_NAME_DEBUG
        } else {
            DatabaseHelper.APP_DATABASE_NAME_PROD
        },
    )

    return FeedFlowDB(
        sqlDriver,
        cloud_pending_article_flagAdapter = Cloud_pending_article_flag.Adapter(
            field_Adapter = EnumColumnAdapter(),
        ),
        cloud_pending_feed_or_category_changeAdapter = Cloud_pending_feed_or_category_change.Adapter(
            entityAdapter = EnumColumnAdapter(),
            field_Adapter = EnumColumnAdapter(),
        ),
        feed_source_cache_infoAdapter = Feed_source_cache_info.Adapter(
            user_agent_tierAdapter = EnumColumnAdapter(),
        ),
        feed_sourceAdapter = Feed_source.Adapter(
            positionAdapter = IntColumnAdapter,
        ),
        feed_source_preferencesAdapter = Feed_source_preferences.Adapter(
            article_open_modeAdapter = EnumColumnAdapter(),
            pinned_positionAdapter = IntColumnAdapter,
        ),
        feed_source_categoryAdapter = Feed_source_category.Adapter(
            positionAdapter = IntColumnAdapter,
        ),
        feed_item_statusAdapter = Feed_item_status.Adapter(
            typeAdapter = EnumColumnAdapter(),
        ),
    )
}

fun getFeedItems(
    appEnvironment: AppEnvironment,
    filterType: String,
    filterId: String?,
): List<FeedItemWidget> {
    val isBookmarksFilter = filterType == "bookmarks"
    return createWidgetDatabase(appEnvironment)
        .feedItemQueries
        .selectFeedsForWidget(
            isBookmarked = true.takeIf { isBookmarksFilter },
            isRead = false.takeUnless { isBookmarksFilter },
            feedSourceId = filterId.takeIf { filterType == "source" },
            feedSourceCategoryId = filterId.takeIf { filterType == "category" },
            pageSize = 6,
        )
        .executeAsList()
        .map { item ->
            FeedItemWidget(
                id = item.url_hash,
                title = item.title,
                subtitle = item.subtitle,
                imageUrl = item.image_url,
                feedSourceTitle = item.feed_source_title,
            )
        }
}

fun getWidgetContentOptions(appEnvironment: AppEnvironment): List<WidgetContentOption> {
    val dbRef = createWidgetDatabase(appEnvironment)
    val categories = dbRef.feedSourceCategoryQueries
        .selectAll()
        .executeAsList()
        .map { category ->
            WidgetContentOption(
                id = category.id,
                title = category.title,
                subtitle = null,
                isCategory = true,
            )
        }
    val sources = dbRef.feedSourceQueries
        .selectFeedUrls()
        .executeAsList()
        .map { source ->
            WidgetContentOption(
                id = source.url_hash,
                title = source.feed_source_title,
                subtitle = source.url,
                isCategory = false,
                logoUrl = source.feed_source_logo_url,
            )
        }
    return categories + sources
}

fun getWidgetStrings(
    languageCode: String?,
    regionCode: String?,
): WidgetStrings {
    val strings = when {
        languageCode == null -> EnFeedFlowStrings
        regionCode == null -> feedFlowStrings[languageCode] ?: EnFeedFlowStrings
        else -> {
            val locale = "${languageCode}_$regionCode"
            feedFlowStrings[locale] ?: feedFlowStrings[languageCode] ?: EnFeedFlowStrings
        }
    }
    return WidgetStrings(
        widgetTitle = strings.widgetLatestItems,
        widgetEmptyScreenTitle = strings.emptyFeedMessage,
        widgetEmptyScreenContent = strings.widgetCheckFeedSources,
        widgetContentTimeline = strings.widgetLatestItems,
        widgetContentBookmarks = strings.drawerTitleBookmarks,
        widgetContentSectionTitle = strings.widgetContentSectionTitle,
        widgetContentCategory = strings.drawerTitleCategories,
        widgetContentFeedSource = strings.drawerTitleFeedSources,
        widgetBookmarksEmptyMessage = strings.bookmarkedArticlesEmptyScreenMessage,
    )
}

data class FeedItemWidget(
    val id: String,
    val title: String?,
    val subtitle: String?,
    val imageUrl: String?,
    val feedSourceTitle: String,
)

data class WidgetContentOption(
    val id: String,
    val title: String,
    val subtitle: String?,
    val isCategory: Boolean,
    val logoUrl: String? = null,
)

data class WidgetStrings(
    val widgetTitle: String,
    val widgetEmptyScreenTitle: String,
    val widgetEmptyScreenContent: String,
    val widgetContentTimeline: String,
    val widgetContentBookmarks: String,
    val widgetContentSectionTitle: String,
    val widgetContentCategory: String,
    val widgetContentFeedSource: String,
    val widgetBookmarksEmptyMessage: String,
)
