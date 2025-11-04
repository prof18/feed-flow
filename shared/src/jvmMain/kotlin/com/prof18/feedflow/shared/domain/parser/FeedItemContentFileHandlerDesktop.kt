package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import kotlinx.coroutines.withContext
import java.io.File

internal class FeedItemContentFileHandlerDesktop(
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
    private val appEnvironment: AppEnvironment,
) : FeedItemContentFileHandler {

    private fun getArticlesDirectory(): File {
        val feedFlowDir = AppDataPathBuilder.getAppDataPath(appEnvironment)
        val articlesDir = File(feedFlowDir, ARTICLES_DIR)
        if (!articlesDir.exists()) {
            articlesDir.mkdirs()
        }
        return articlesDir
    }

    private fun getArticleFile(feedItemId: String): File {
        return File(getArticlesDirectory(), "$feedItemId.html")
    }

    override suspend fun saveFeedItemContentToFile(feedItemId: String, content: String) {
        withContext(dispatcherProvider.io) {
            try {
                val file = getArticleFile(feedItemId)
                file.writeText(content)
                logger.d { "Saved content for feed item: $feedItemId (${content.length} bytes)" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to save content for feed item: $feedItemId" }
            }
        }
    }

    override suspend fun loadFeedItemContent(feedItemId: String): String? {
        return withContext(dispatcherProvider.io) {
            try {
                val file = getArticleFile(feedItemId)
                if (file.exists()) {
                    val content = file.readText()
                    logger.d { "Loaded content for feed item: $feedItemId (${content.length} bytes)" }
                    content
                } else {
                    logger.d { "No cached content for feed item: $feedItemId" }
                    null
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to load content for feed item: $feedItemId" }
                null
            }
        }
    }

    override suspend fun isContentAvailable(feedItemId: String): Boolean {
        return withContext(dispatcherProvider.io) {
            try {
                val file = getArticleFile(feedItemId)
                file.exists()
            } catch (e: Exception) {
                logger.e(e) { "Failed to check content availability for feed item: $feedItemId" }
                false
            }
        }
    }

    override suspend fun deleteFeedItemContent(feedItemId: String) {
        withContext(dispatcherProvider.io) {
            try {
                val file = getArticleFile(feedItemId)
                if (file.exists()) {
                    file.delete()
                    logger.d { "Deleted content for feed item: $feedItemId" }
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to delete content for feed item: $feedItemId" }
            }
        }
    }

    override suspend fun clearAllContent() {
        withContext(dispatcherProvider.io) {
            try {
                val dir = getArticlesDirectory()
                val deletedCount = dir.listFiles()?.count { it.delete() } ?: 0
                logger.d { "Cleared all cached content ($deletedCount files)" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to clear all content" }
            }
        }
    }

    companion object {
        private const val ARTICLES_DIR = "articles"
    }
}
