@file:Suppress("MaxLineLength")

package com.prof18.feedflow.presentation.preview

import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoriesState.CategoryItem
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.domain.model.Browser

val feedItemsForPreview = listOf(
    FeedItem(
        id = 0,
        url = "https://www.ilpost.it/2023/02/11/scheumorfismo/",
        title = "Le forme e gli oggetti che ci portiamo dietro nonostante il progresso",
        subtitle = null,
        content = null,
        imageUrl = "",
        feedSource = FeedSource(
            id = 1,
            url = "https://www.ilpost.it",
            title = "Windows Central - News, Forums, Reviews, Help for Windows",
            category = FeedSourceCategory(
                id = 2,
                title = "Tech",
            ),
            lastSyncTimestamp = null,
        ),
        isRead = false,
        pubDateMillis = null,
        dateString = null,
        commentsUrl = null,
    ),
    FeedItem(
        id = 1,
        url = "https://www.androidpolice.com/google-pixel-7-pro-vs-pixel-6-pro/",
        title = "Google Pixel 7 Pro vs. Pixel 6 Pro: Should you upgrade?",
        subtitle = "The Pixel 7 Pro might not be a dramatic overhaul the way the 6 Pro was, but small refinements elevate the experience",
        content = null,
        imageUrl = "https://static1.anpoimages.com/wordpress/wp-content/uploads/2022/10/Pixel-7-Pro-vs-Pixel-6-Pro-comparison.jpg",
        feedSource = FeedSource(
            id = 2,
            url = "",
            title = "Android Police",
            lastSyncTimestamp = null,
            category = FeedSourceCategory(
                id = 2,
                title = "Tech",
            ),
        ),
        isRead = true,
        pubDateMillis = 1675890077000,
        dateString = "12/02 - 16:22",
        commentsUrl = null,
    ),
    FeedItem(
        id = 3,
        url = "https://9to5linux.com/obs-studio-29-0-1-is-out-to-fix-linux-crash-on-wayland-x11-capture-issue",
        title = "OBS Studio 29.0.1 Is Out to Fix Linux Crash on Wayland, X11 Capture Issue",
        subtitle = "<p>OBS Studio 29.0.1 open-source and free software for live streaming and screen recording is now available for download with several bug fixes.</p> <p>The post <a rel=\"nofollow\" href=\"https://9to5linux.com/obs-studio-29-0-1-is-out-to-fix-linux-crash-on-wayland-x11-capture-issue\">OBS Studio 29.0.1 Is Out to Fix Linux Crash on Wayland, X11 Capture Issue</a> appeared first on <a rel=\"nofollow\" href=\"https://9to5linux.com\">9to5Linux</a> - do not reproduce this article without permission. This RSS feed is intended for readers, not scrapers.</p>",
        content = null,
        imageUrl = null,
        feedSource = FeedSource(
            id = 3,
            url = "https://9to5linux.com",
            title = "9to5 Linux",
            lastSyncTimestamp = null,
            category = FeedSourceCategory(
                id = 2,
                title = "Tech",
            ),
        ),
        isRead = false,
        pubDateMillis = 0,
        dateString = "12/12 - 9:22",
        commentsUrl = null,
    ),
    FeedItem(
        id = 4,
        url = "https://androiddev.social/@marcogom/111096537433708200",
        title = null,
        subtitle = "<p>Back to writing after a while!</p><p>In this article, I cover how I used the HiddenFromObjC and ObjCName annotations introduced with <a href=\"https://androiddev.social/tags/Kotlin\" class=\"mention hashtag\" rel=\"tag\">#<span>Kotlin</span></a> 1.8 to improve the architecture of MoneyFlow!</p><p><a href=\"https://www.marcogomiero.com/posts/2023/objc-annotiations-better-kmp-api/\" target=\"_blank\" rel=\"nofollow noopener noreferrer\"><span class=\"invisible\">https://www.</span><span class=\"ellipsis\">marcogomiero.com/posts/2023/ob</span><span class=\"invisible\">jc-annotiations-better-kmp-api/</span></a></p>",
        content = null,
        imageUrl = null,
        feedSource = FeedSource(
            id = 3,
            url = "https://9to5linux.com",
            title = "9to5 Linux",
            lastSyncTimestamp = null,
            category = FeedSourceCategory(
                id = 2,
                title = "Tech",
            ),
        ),
        isRead = false,
        pubDateMillis = 0,
        dateString = "12/12 - 9:22",
        commentsUrl = null,
    ),
)

val browsersForPreview = listOf(
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

val feedSourcesForPreview = listOf(
    FeedSource(
        id = 0,
        url = "https://www.site1.com",
        title = "Site 1",
        lastSyncTimestamp = null,
        category = FeedSourceCategory(
            id = 2,
            title = "Tech",
        ),
    ),
    FeedSource(
        id = 1,
        url = "https://www.site2.com",
        title = "Site 2",
        lastSyncTimestamp = null,
        category = FeedSourceCategory(
            id = 1,
            title = "News",
        ),
    ),
    FeedSource(
        id = 2,
        url = "https://www.site3.com",
        title = "Site 3",
        lastSyncTimestamp = null,
        category = FeedSourceCategory(
            id = 2,
            title = "Tech",
        ),
    ),
)

val importExportStates = listOf(
    FeedImportExportState.Idle,
    FeedImportExportState.Error,
    FeedImportExportState.LoadingImport,
    FeedImportExportState.LoadingExport,
    FeedImportExportState.ExportSuccess,
    FeedImportExportState.ImportSuccess(
        notValidFeedSources = emptyList(),
    ),
    FeedImportExportState.ImportSuccess(
        notValidFeedSources = listOf(
            ParsedFeedSource(
                url = "https://www.ilpost.it",
                title = "Il Post",
                categoryName = null,
            ),
            ParsedFeedSource(
                url = "https://www.techcrunch.it",
                title = "Tech Crunch",
                categoryName = null,
            ),
        ),
    ),
)

val categoriesState = CategoriesState(
    isExpanded = true,
    header = "No category selected",
    categories = listOf(
        CategoryItem(
            id = 0,
            name = "Android",
            isSelected = true,
            onClick = {},
        ),
        CategoryItem(
            id = 0,
            name = "Apple",
            isSelected = false,
            onClick = {},
        ),
        CategoryItem(
            id = 0,
            name = "Tech",
            isSelected = false,
            onClick = {},
        ),
    ),
)

val feedSourcesState = listOf(
    FeedSourceState(
        categoryId = CategoryId(1),
        categoryName = "Tech",
        isExpanded = true,
        feedSources = feedSourcesForPreview,
    ),
    FeedSourceState(
        categoryId = CategoryId(1),
        categoryName = "News",
        isExpanded = false,
        feedSources = feedSourcesForPreview,
    ),
    FeedSourceState(
        categoryId = CategoryId(1),
        categoryName = "Mobile",
        isExpanded = true,
        feedSources = feedSourcesForPreview,
    ),
)

val drawerItemsForPreview = listOf(
    DrawerItem.Timeline,

    DrawerItem.CategorySectionTitle,

    DrawerItem.DrawerCategory(
        category = FeedSourceCategory(
            id = 2414,
            title = "News",
        ),
    ),

    DrawerItem.DrawerCategory(
        category = FeedSourceCategory(
            id = 2415,
            title = "Tech",
        ),
    ),

    DrawerItem.DrawerCategory(
        category = FeedSourceCategory(
            id = 2416,
            title = "Basket",
        ),
    ),

    DrawerItem.CategorySourcesTitle,

    DrawerItem.DrawerCategoryWrapper(
        category = FeedSourceCategory(
            id = 9398,
            title = "News",
        ),
        feedSources = listOf(
            DrawerItem.DrawerCategoryWrapper.FeedSourceWrapper(
                feedSource = feedSourcesForPreview[0],
            ),
            DrawerItem.DrawerCategoryWrapper.FeedSourceWrapper(
                feedSource = feedSourcesForPreview[1],
            ),
            DrawerItem.DrawerCategoryWrapper.FeedSourceWrapper(
                feedSource = feedSourcesForPreview[2],
            ),
        ),
        isExpanded = false,
        onExpandClick = {},
    ),

    DrawerItem.DrawerCategoryWrapper(
        category = FeedSourceCategory(
            id = 9398,
            title = "News",
        ),
        feedSources = listOf(
            DrawerItem.DrawerCategoryWrapper.FeedSourceWrapper(
                feedSource = feedSourcesForPreview[0],
            ),
            DrawerItem.DrawerCategoryWrapper.FeedSourceWrapper(
                feedSource = feedSourcesForPreview[1],
            ),
            DrawerItem.DrawerCategoryWrapper.FeedSourceWrapper(
                feedSource = feedSourcesForPreview[2],
            ),
        ),
        isExpanded = true,
        onExpandClick = {},
    ),
)
