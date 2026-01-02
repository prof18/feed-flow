package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataWithContentsOfFile

internal class FeedItemContentFileHandlerIos(
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) : FeedItemContentFileHandler {

    override suspend fun saveFeedItemContentToFile(feedItemId: String, content: String) {
        withContext(dispatcherProvider.io) {
            try {
                // Ensure articles directory exists
                val articlesPath = getArticlesFolderPath()
                if (articlesPath != null) {
                    val fileManager = NSFileManager.defaultManager
                    if (!fileManager.fileExistsAtPath(articlesPath)) {
                        fileManager.createDirectoryAtPath(
                            path = articlesPath,
                            withIntermediateDirectories = true,
                            attributes = null,
                            error = null,
                        )
                    }
                }

                val path = getArticleFilePath(feedItemId)

                @Suppress("CAST_NEVER_SUCCEEDS")
                val contentData = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: NSData()

                if (path != null) {
                    NSFileManager.defaultManager.createFileAtPath(
                        path = path,
                        contents = contentData,
                        attributes = null,
                    )
                }
            } catch (e: Throwable) {
                logger.e(e) { "Error saving content to file" }
            }
        }
    }

    override suspend fun loadFeedItemContent(feedItemId: String): String? {
        return withContext(dispatcherProvider.io) {
            try {
                val path = getArticleFilePath(feedItemId) ?: return@withContext null
                val data = NSData.dataWithContentsOfFile(path) ?: return@withContext null
                return@withContext NSString.create(data, NSUTF8StringEncoding).toString()
            } catch (e: Throwable) {
                logger.e(e) { "Error loading content from file" }
                return@withContext null
            }
        }
    }

    override suspend fun isContentAvailable(feedItemId: String): Boolean {
        return withContext(dispatcherProvider.io) {
            val path = getArticleFilePath(feedItemId) ?: return@withContext false
            return@withContext NSFileManager.defaultManager.fileExistsAtPath(path)
        }
    }

    override suspend fun deleteFeedItemContent(feedItemId: String) {
        withContext(dispatcherProvider.io) {
            getArticleFilePath(feedItemId)?.let { path ->
                NSFileManager.defaultManager.removeItemAtPath(
                    path = path,
                    error = null,
                )
            }
        }
    }

    override suspend fun clearAllContent() {
        withContext(dispatcherProvider.io) {
            getArticlesFolderPath()?.let { path ->
                NSFileManager.defaultManager.removeItemAtPath(
                    path = path,
                    error = null,
                )
            }
        }
    }

    private fun getArticleFilePath(feedItemId: String): String? =
        getArticlesFolderURL()
            ?.URLByAppendingPathComponent("$feedItemId.html")
            ?.path

    private fun getArticlesFolderPath(): String? =
        getArticlesFolderURL()
            ?.path

    private fun getArticlesFolderURL(): NSURL? =
        NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier("group.com.prof18.feedflow")
            ?.URLByAppendingPathComponent("articles")
}
