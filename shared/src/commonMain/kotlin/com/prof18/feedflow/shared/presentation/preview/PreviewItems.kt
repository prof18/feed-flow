@file:Suppress("MaxLineLength")

package com.prof18.feedflow.shared.presentation.preview

import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoriesState.CategoryItem
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.shared.domain.model.Browser
import kotlinx.collections.immutable.persistentListOf

val browsersForPreview = persistentListOf(
    Browser(
        id = "",
        name = "Chrome",
        isFavourite = false,
    ),
    Browser(
        id = "",
        name = "Firefox",
        isFavourite = true,
    ),
)

val feedSourcesForPreview = persistentListOf(
    FeedSource(
        id = "0",
        url = "https://www.site1.site1.site1.site1.site1.site1.site1.site1.site1.site1.com",
        title = "Site 1",
        lastSyncTimestamp = null,
        category = FeedSourceCategory(
            id = "2",
            title = "Tech",
        ),
        logoUrl = null,
        websiteUrl = null,
        isHiddenFromTimeline = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isPinned = false,
        isNotificationEnabled = false,
        fetchFailed = false,
    ),
    FeedSource(
        id = "1",
        url = "https://www.site2.com",
        title = "Site 2",
        lastSyncTimestamp = null,
        category = FeedSourceCategory(
            id = "1",
            title = "News",
        ),
        logoUrl = null,
        websiteUrl = null,
        isHiddenFromTimeline = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isPinned = false,
        isNotificationEnabled = false,
        fetchFailed = false,
    ),
    FeedSource(
        id = "2",
        url = "https://www.site3.com",
        title = "Site 3",
        lastSyncTimestamp = null,
        category = FeedSourceCategory(
            id = "2",
            title = "Tech",
        ),
        logoUrl = null,
        websiteUrl = null,
        isHiddenFromTimeline = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isPinned = false,
        isNotificationEnabled = false,
        fetchFailed = false,
    ),
)

val importExportStates = listOf(
    FeedImportExportState.Idle,
    FeedImportExportState.Error,
    FeedImportExportState.LoadingImport(ImportExportContentType.FeedsOpml),
    FeedImportExportState.LoadingExport(ImportExportContentType.FeedsOpml),
    FeedImportExportState.ExportSuccess,
    FeedImportExportState.ImportSuccess(
        notValidFeedSources = persistentListOf(),
        feedSourceWithError = persistentListOf(),
    ),
    FeedImportExportState.ArticleExportSuccess,
    FeedImportExportState.ArticleImportSuccess,
    FeedImportExportState.ImportSuccess(
        feedSourceWithError = persistentListOf(),
        notValidFeedSources = persistentListOf(
            ParsedFeedSource(
                id = "1",
                url = "https://www.ilpost.it",
                title = "Il Post",
                category = null,
                logoUrl = null,
                websiteUrl = null,
            ),
            ParsedFeedSource(
                id = "1",
                url = "https://www.techcrunch.it",
                title = "Tech Crunch",
                category = null,
                logoUrl = null,
                websiteUrl = null,
            ),
        ),
    ),
)

val categoriesExpandedState = CategoriesState(
    categories = persistentListOf(
        CategoryItem(
            id = "0",
            name = "Android",
            isSelected = true,
        ),
        CategoryItem(
            id = "0",
            name = "Apple",
            isSelected = false,
        ),
        CategoryItem(
            id = "0",
            name = "Tech",
            isSelected = false,
        ),
    ),
)

val feedSourcesState = persistentListOf(
    FeedSourceState(
        categoryId = CategoryId("1"),
        categoryName = "Tech",
        isExpanded = true,
        feedSources = feedSourcesForPreview,
    ),
    FeedSourceState(
        categoryId = CategoryId("1"),
        categoryName = "News",
        isExpanded = false,
        feedSources = feedSourcesForPreview,
    ),
    FeedSourceState(
        categoryId = CategoryId("1"),
        categoryName = "Mobile",
        isExpanded = true,
        feedSources = feedSourcesForPreview,
    ),
)
