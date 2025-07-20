package com.prof18.feedflow.shared.di

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.getAppGroupDatabasePath
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.db.Feed_item_status
import com.prof18.feedflow.db.Feed_source_preferences
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings

fun getFeedItems(appEnvironment: AppEnvironment): List<FeedItemWidget> {
    val sqlDriver = NativeSqliteDriver(
        schema = FeedFlowDB.Schema,
        onConfiguration = { conf ->
            conf.copy(
                extendedConfig = conf.extendedConfig.copy(
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

    val dbRef = FeedFlowDB(
        sqlDriver,
        feed_source_preferencesAdapter = Feed_source_preferences.Adapter(
            link_opening_preferenceAdapter = EnumColumnAdapter(),
        ),
        feed_item_statusAdapter = Feed_item_status.Adapter(
            typeAdapter = EnumColumnAdapter(),
        ),
    )

    return dbRef.feedItemQueries.selectFeedsForWidget(pageSize = 6).executeAsList()
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

fun getWidgetStrings(
    languageCode: String?,
    regionCode: String?,
): WidgetStrings {
    val feedFlowStrings = when {
        languageCode == null -> EnFeedFlowStrings
        regionCode == null -> feedFlowStrings[languageCode] ?: EnFeedFlowStrings
        else -> {
            val locale = "${languageCode}_$regionCode"
            feedFlowStrings[locale] ?: feedFlowStrings[languageCode] ?: EnFeedFlowStrings
        }
    }
    return WidgetStrings(
        widgetTitle = feedFlowStrings.widgetLatestItems,
        widgetEmptyScreenTitle = feedFlowStrings.emptyFeedMessage,
        widgetEmptyScreenContent = feedFlowStrings.widgetCheckFeedSources,
    )
}

data class FeedItemWidget(
    val id: String,
    val title: String?,
    val subtitle: String?,
    val imageUrl: String?,
    val feedSourceTitle: String,
)

data class WidgetStrings(
    val widgetTitle: String,
    val widgetEmptyScreenTitle: String,
    val widgetEmptyScreenContent: String,
)
