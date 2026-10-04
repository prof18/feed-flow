package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedItem
import kotlinx.collections.immutable.ImmutableList

internal class ArticleNavigationRepository(
    private val feedStateRepository: FeedStateRepository,
) {
    private var searchResults: ImmutableList<FeedItem>? = null

    fun setSearchResults(items: ImmutableList<FeedItem>) {
        searchResults = items
    }

    fun clearSearchResults() {
        searchResults = null
    }

    suspend fun getNextArticle(currentArticleId: String): FeedItem? {
        val searchList = searchListContaining(currentArticleId)
            ?: return feedStateRepository.getNextArticle(currentArticleId)
        val index = searchList.indexOfFirst { it.id == currentArticleId }
        return searchList.getOrNull(index + 1)
    }

    fun getPreviousArticle(currentArticleId: String): FeedItem? {
        val searchList = searchListContaining(currentArticleId)
            ?: return feedStateRepository.getPreviousArticle(currentArticleId)
        val index = searchList.indexOfFirst { it.id == currentArticleId }
        return if (index > 0) searchList.getOrNull(index - 1) else null
    }

    fun getArticlePosition(currentArticleId: String): ArticlePosition? {
        val searchList = searchListContaining(currentArticleId)
            ?: return feedStateRepository.getArticlePosition(currentArticleId)
        val index = searchList.indexOfFirst { it.id == currentArticleId }
        return ArticlePosition(currentPosition = index + 1, totalArticles = searchList.size)
    }

    private fun searchListContaining(articleId: String): ImmutableList<FeedItem>? {
        val list = searchResults ?: return null
        return list.takeIf { items -> items.any { it.id == articleId } }
    }
}
