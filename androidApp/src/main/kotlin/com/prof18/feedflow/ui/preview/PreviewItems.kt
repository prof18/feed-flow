package com.prof18.feedflow.ui.preview

import com.prof18.feedflow.domain.model.Browser
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.domain.model.FeedSource

internal val feedItemsForPreview = listOf(
    FeedItem(
        id = 0,
        url = "https://www.ilpost.it/2023/02/11/scheumorfismo/",
        title = "Le forme e gli oggetti che ci portiamo dietro nonostante il progresso",
        subtitle = null,
        content = null,
        imageUrl = "",
        feedSource = FeedSource(id = 1, url = "https://www.ilpost.it", title = "Il Post"),
        isRead = false,
        pubDateMillis = 1676107668000,
        dateString = "16:22",
        commentsUrl = null,
    ),
    FeedItem(
        id = 1,
        url = "https://www.androidpolice.com/google-pixel-7-pro-vs-pixel-6-pro/",
        title = "Google Pixel 7 Pro vs. Pixel 6 Pro: Should you upgrade?",
        subtitle = "The Pixel 7 Pro might not be a dramatic overhaul the way the 6 Pro was, but small refinements elevate the experience",
        content = null,
        imageUrl = "https://static1.anpoimages.com/wordpress/wp-content/uploads/2022/10/Pixel-7-Pro-vs-Pixel-6-Pro-comparison.jpg",
        feedSource = FeedSource(id = 2, url = "", title = "Android Police"),
        isRead = true,
        pubDateMillis = 1675890077000,
        dateString = "12/02 - 16:22",
        commentsUrl = null,
    ),
    FeedItem(
        id = 2,
        url = "https://9to5linux.com/obs-studio-29-0-1-is-out-to-fix-linux-crash-on-wayland-x11-capture-issue",
        title = "OBS Studio 29.0.1 Is Out to Fix Linux Crash on Wayland, X11 Capture Issue",
        subtitle = "<p>OBS Studio 29.0.1 open-source and free software for live streaming and screen recording is now available for download with several bug fixes.</p> <p>The post <a rel=\"nofollow\" href=\"https://9to5linux.com/obs-studio-29-0-1-is-out-to-fix-linux-crash-on-wayland-x11-capture-issue\">OBS Studio 29.0.1 Is Out to Fix Linux Crash on Wayland, X11 Capture Issue</a> appeared first on <a rel=\"nofollow\" href=\"https://9to5linux.com\">9to5Linux</a> - do not reproduce this article without permission. This RSS feed is intended for readers, not scrapers.</p>",
        content = null,
        imageUrl = null,
        feedSource = FeedSource(id = 3, url = "https://9to5linux.com", title = "9to5 Linux"),
        isRead = false,
        pubDateMillis = 0,
        dateString = "12/12 - 9:22",
        commentsUrl = null,
    ),
)

internal val browsersForPreview = listOf(
    Browser(
        id = "",
        name = "Chrome",
        isFavourite = false
    ),
    Browser(
        id = "",
        name = "Firefox",
        isFavourite = true
    )
)

internal val feedSourcesForPreview = listOf(
    FeedSource(
        id = 0,
        url = "https://www.site1.com",
        title = "Site 1"
    ),
    FeedSource(
        id = 1,
        url = "https://www.site2.com",
        title = "Site 2"
    ),
    FeedSource(
        id = 2,
        url = "https://www.site3.com",
        title = "Site 3"
    )
)