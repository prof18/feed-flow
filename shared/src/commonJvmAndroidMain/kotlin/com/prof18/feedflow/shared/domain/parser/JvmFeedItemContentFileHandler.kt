package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.StagedReaderContent
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

internal class JvmFeedItemContentFileHandler(
    private val articlesDirectory: File,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) : FeedItemContentFileHandler {
    private val counters = Any()
    private var cacheGeneration = 0L
    private val strayStageCleanup = Any()

    @Volatile
    private var strayStagesDeleted = false

    private fun getArticlesDirectory(): File {
        if (!articlesDirectory.exists()) {
            articlesDirectory.mkdirs()
        }
        // Callers wait for the cleanup so it cannot delete a stage file written concurrently
        if (!strayStagesDeleted) {
            synchronized(strayStageCleanup) {
                if (!strayStagesDeleted) {
                    articlesDirectory.listFiles { file -> file.isStageFile() }?.forEach { it.delete() }
                    strayStagesDeleted = true
                }
            }
        }
        return articlesDirectory
    }

    override suspend fun stageContent(feedItemId: String, content: String): StagedReaderContent? =
        withContext(dispatcherProvider.io) {
            val generation = synchronized(counters) { cacheGeneration }
            val file = File(getArticlesDirectory(), "$STAGE_PREFIX${UUID.randomUUID()}$STAGE_SUFFIX")
            try {
                file.writeText(content)
                StagedReaderContent(feedItemId, StagedFile(file, generation))
            } catch (e: Exception) {
                logger.e(e) { "Failed to stage content for feed item: $feedItemId" }
                file.delete()
                null
            }
        }

    override suspend fun promoteStaged(staged: StagedReaderContent, commit: (() -> Unit) -> Unit): Boolean =
        withContext(dispatcherProvider.io) {
            val stagedFile = staged.token as? StagedFile ?: return@withContext false
            var promoted = false
            try {
                commit {
                    if (synchronized(counters) { cacheGeneration } != stagedFile.generation) return@commit
                    Files.move(
                        stagedFile.file.toPath(),
                        getArticleFile(staged.feedItemId).toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                    promoted = true
                }
            } catch (e: Exception) {
                logger.d(e) { "Staged content for feed item ${staged.feedItemId} was not promoted" }
            } finally {
                if (!promoted) stagedFile.file.delete()
            }
            promoted
        }

    override suspend fun discardStaged(staged: StagedReaderContent) {
        withContext(dispatcherProvider.io) {
            (staged.token as? StagedFile)?.file?.takeIf { it.exists() }?.delete()
        }
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
        synchronized(counters) { cacheGeneration++ }
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
        internal const val ARTICLES_DIR = "articles"
        private const val STAGE_PREFIX = ".feedflow-reader-"
        private const val STAGE_SUFFIX = ".stage"

        private fun File.isStageFile(): Boolean = name.startsWith(STAGE_PREFIX) && name.endsWith(STAGE_SUFFIX)
    }

    private class StagedFile(val file: File, val generation: Long)
}
