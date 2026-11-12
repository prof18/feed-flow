package com.prof18.feedflow.android.addfeed

import com.prof18.feedflow.core.utils.AddFeedLinkParser
import com.prof18.feedflow.core.utils.SharedTextUrlExtractor
import java.net.URLEncoder

/** The feed URL to add from a share, or null when the shared text and HTML hold no http(s) link. */
internal fun sharedFeedUrl(text: String?, html: String?): String? =
    SharedTextUrlExtractor.firstHttpUrl(text) ?: SharedTextUrlExtractor.firstHttpUrl(html)

/**
 * The link that opens Add feed in FeedFlow prefilled with [feedUrl], or null when
 * MainActivity would reject it, for example because the host is unsafe.
 */
internal fun openInFeedFlowLink(feedUrl: String): String? =
    // The String charset overload: URLEncoder.encode(String, Charset) needs API 33.
    ("feedflow://add?url=" + URLEncoder.encode(feedUrl, "UTF-8"))
        .takeIf { AddFeedLinkParser.parse(it) != null }
