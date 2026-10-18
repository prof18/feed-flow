package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.StagedReaderContent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
internal class FeedItemContentFileHandlerIos(
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
    private val fileManager: NSFileManager = NSFileManager.defaultManager,
    private val articlesFolderUrl: () -> NSURL? = {
        NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier("group.com.prof18.feedflow")
            ?.URLByAppendingPathComponent("articles")
    },
) : FeedItemContentFileHandler {
    private val counters = Lock()
    private var cacheGeneration = 0L
    private val strayStageCleanup = Lock()
    private var strayStagesDeleted = false

    override suspend fun stageContent(feedItemId: String, content: String): StagedReaderContent? =
        withContext(dispatcherProvider.io) {
            val generation = counters.withLock { cacheGeneration }
            val folder = ensureArticlesFolder() ?: return@withContext null
            val stageUrl = folder.URLByAppendingPathComponent("$STAGE_PREFIX${NSUUID().UUIDString}$STAGE_SUFFIX")
            val stagePath = stageUrl?.path ?: return@withContext null
            val data = NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding)
            if (data == null || !fileManager.createFileAtPath(stagePath, data, null)) {
                logger.e { "Failed to stage content for feed item: $feedItemId" }
                fileManager.removeItemAtPath(stagePath, null)
                return@withContext null
            }
            StagedReaderContent(feedItemId, StagedFile(stageUrl, generation))
        }

    override suspend fun promoteStaged(staged: StagedReaderContent, commit: (() -> Unit) -> Unit): Boolean =
        withContext(dispatcherProvider.io) {
            val stagedFile = staged.token as? StagedFile ?: return@withContext false
            val target = getArticlesFolderURL()?.URLByAppendingPathComponent("${staged.feedItemId}.html")
                ?: return@withContext false
            var promoted = false
            try {
                commit {
                    if (counters.withLock { cacheGeneration } != stagedFile.generation) return@commit
                    val targetPath = target.path
                    promoted = if (targetPath != null && fileManager.fileExistsAtPath(targetPath)) {
                        // replaceItemAtURL needs an existing original
                        fileManager.replaceItemAtURL(target, stagedFile.url, null, 0u, null, null)
                    } else {
                        fileManager.moveItemAtURL(stagedFile.url, target, null)
                    }
                }
            } catch (e: Exception) {
                logger.d(e) { "Staged content for feed item ${staged.feedItemId} was not promoted" }
            } finally {
                if (!promoted) stagedFile.url.path?.let { fileManager.removeItemAtPath(it, null) }
            }
            promoted
        }

    override suspend fun discardStaged(staged: StagedReaderContent) {
        withContext(dispatcherProvider.io) {
            val path = (staged.token as? StagedFile)?.url?.path ?: return@withContext
            if (fileManager.fileExistsAtPath(path)) {
                fileManager.removeItemAtPath(path, null)
            }
        }
    }

    private fun ensureArticlesFolder(): NSURL? {
        val folder = getArticlesFolderURL() ?: return null
        val path = folder.path ?: return null
        if (!fileManager.fileExistsAtPath(path)) {
            fileManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        }
        // Callers wait for the cleanup so it cannot delete a stage file written concurrently
        strayStageCleanup.withLock {
            if (!strayStagesDeleted) {
                fileManager.contentsOfDirectoryAtPath(path, null)
                    ?.filterIsInstance<String>()
                    ?.filter { it.startsWith(STAGE_PREFIX) && it.endsWith(STAGE_SUFFIX) }
                    ?.forEach { fileManager.removeItemAtPath("$path/$it", null) }
                strayStagesDeleted = true
            }
        }
        return folder
    }

    override suspend fun saveFeedItemContentToFile(feedItemId: String, content: String) {
        withContext(dispatcherProvider.io) {
            try {
                // Ensure articles directory exists
                val articlesPath = getArticlesFolderPath()
                if (articlesPath != null) {
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

                val contentData = NSString.create(string = content)
                    .dataUsingEncoding(NSUTF8StringEncoding)
                    ?: NSData()

                if (path != null) {
                    fileManager.createFileAtPath(
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
            return@withContext fileManager.fileExistsAtPath(path)
        }
    }

    override suspend fun deleteFeedItemContent(feedItemId: String) {
        withContext(dispatcherProvider.io) {
            getArticleFilePath(feedItemId)?.let { path ->
                fileManager.removeItemAtPath(
                    path = path,
                    error = null,
                )
            }
        }
    }

    override suspend fun clearAllContent() {
        counters.withLock { cacheGeneration++ }
        withContext(dispatcherProvider.io) {
            getArticlesFolderPath()?.let { path ->
                fileManager.removeItemAtPath(
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

    private fun getArticlesFolderURL(): NSURL? = articlesFolderUrl()

    private class StagedFile(val url: NSURL, val generation: Long)

    private companion object {
        const val STAGE_PREFIX = ".feedflow-reader-"
        const val STAGE_SUFFIX = ".stage"
    }
}
