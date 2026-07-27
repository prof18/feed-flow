package com.prof18.feedflow.android.feed

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ArticleOpenMode.DEFAULT
import com.prof18.feedflow.core.model.ArticleOpenMode.FEED_CONTENT
import com.prof18.feedflow.core.model.ArticleOpenMode.FULL_ARTICLE
import com.prof18.feedflow.core.model.ArticleOpenMode.INTERNAL_BROWSER
import com.prof18.feedflow.core.model.ArticleOpenMode.PREFERRED_BROWSER

internal object ArticleOpenModeE2eIds {
    fun option(mode: ArticleOpenMode): String = when (mode) {
        DEFAULT -> "article_open_mode_default"
        FULL_ARTICLE -> "article_open_mode_full_article"
        FEED_CONTENT -> "article_open_mode_feed_content"
        INTERNAL_BROWSER -> "article_open_mode_internal_browser"
        PREFERRED_BROWSER -> "article_open_mode_preferred_browser"
    }
}
