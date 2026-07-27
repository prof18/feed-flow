package com.prof18.feedflow.core.model

/**
 * How an article opens, both as the app-wide default and as a per-feed override.
 *
 * [DEFAULT] is only meaningful as a per-feed value: it means "follow the global setting".
 * Resolve a feed value against the global one with [resolveWith] before acting on it.
 */
enum class ArticleOpenMode {
    DEFAULT,
    FULL_ARTICLE,
    FEED_CONTENT,
    INTERNAL_BROWSER,
    PREFERRED_BROWSER,
}

/** The modes a user can pick as the global default: everything but [ArticleOpenMode.DEFAULT]. */
val globalArticleOpenModes: List<ArticleOpenMode> =
    ArticleOpenMode.entries - ArticleOpenMode.DEFAULT

/**
 * How the app behaves before the user chooses anything: reader mode showing the full article.
 * This is what [ArticleOpenMode.DEFAULT] resolves to once it reaches the global level.
 */
val appDefaultArticleOpenMode: ArticleOpenMode = ArticleOpenMode.FULL_ARTICLE

/** Resolves a per-feed value against the global default. */
fun ArticleOpenMode.resolveWith(globalDefault: ArticleOpenMode): ArticleOpenMode =
    if (this == ArticleOpenMode.DEFAULT) globalDefault else this

/** Whether the mode opens the article in the reader instead of a browser. */
fun ArticleOpenMode.isReaderMode(): Boolean =
    this == ArticleOpenMode.FULL_ARTICLE || this == ArticleOpenMode.FEED_CONTENT
