package com.prof18.feedflow.android.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.R
import com.prof18.feedflow.core.model.ArticleNotificationBatch
import com.prof18.feedflow.core.model.ArticleNotificationPolicy
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.notification.ArticleNotifier
import com.prof18.feedflow.shared.domain.notification.Notifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*

class AndroidNotifier(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val imageLoader: suspend (String?, Long) -> Bitmap? = AndroidNotificationImageLoader(context)::load,
    private val monotonicMillis: () -> Long = SystemClock::elapsedRealtime,
    private val activeNotificationKeys: () -> Set<Pair<String?, Int>> = {
        context.getSystemService(NotificationManager::class.java).activeNotifications.map { it.tag to it.id }.toSet()
    },
    // Callers check areNotificationsEnabled() before posting, as showNewArticlesNotification does.
    @SuppressLint("MissingPermission")
    private val postArticleNotification: (String, Notification) -> Unit = { tag, notification ->
        NotificationManagerCompat.from(context).notify(tag, 0, notification)
    },
) : Notifier, ArticleNotifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    private val articleMutex = Mutex()

    override suspend fun showArticleNotifications(
        prepareBatch: suspend () -> ArticleNotificationBatch,
        acknowledge: suspend (List<String>) -> Unit,
    ) = articleMutex.withLock {
        if (!notificationManager.areNotificationsEnabled()) return@withLock
        createNotificationChannel()
        if (!isNotificationChannelEnabled()) return@withLock
        val initialKeys = queryActiveKeys() ?: return@withLock
        val batch = fitToActiveCapacity(prepareBatch(), initialKeys) ?: return@withLock

        val strings = feedFlowStrings()
        val postedKeys = mutableSetOf<Pair<String?, Int>>()
        var submitted = false
        var imageElapsed = 0L

        suspend fun submit(tag: String, notification: Notification, ids: List<String>): Boolean {
            currentCoroutineContext().ensureActive()
            val activeKeys = queryActiveKeys() ?: return false
            val key = tag to 0
            if ((activeKeys + postedKeys + key).size > ArticleNotificationPolicy.ANDROID_ACTIVE_SOFT_LIMIT) return false
            try {
                postArticleNotification(tag, notification)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return true
            }
            postedKeys += key
            submitted = true
            withContext(NonCancellable) { acknowledge(ids) }
            return true
        }

        for (article in batch.articles) {
            currentCoroutineContext().ensureActive()
            val allowance = minOf(
                ArticleNotificationPolicy.IMAGE_TIMEOUT_MS,
                (ArticleNotificationPolicy.IMAGE_BATCH_BUDGET_MS - imageElapsed).coerceAtLeast(0),
            )
            val bitmap = if (allowance > 0) {
                val start = monotonicMillis()
                val loaded = loadArticleImage(article.imageUrl, allowance)
                imageElapsed += (monotonicMillis() - start).coerceAtLeast(0)
                loaded
            } else {
                null
            }
            val tag = ArticleNotificationPolicy.ARTICLE_ID_PREFIX + article.id
            val url = Uri.Builder().scheme("feedflow").authority("feed").appendPath(article.id).build()
            val builder = articleNotificationBuilder(
                title = articleTitle(article.title, strings),
                body = article.feedSourceTitle,
                url = url,
                silent = submitted,
            )
            if (bitmap != null) {
                builder.setLargeIcon(bitmap).setStyle(
                    NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?),
                )
            }
            if (!submit(tag, builder.build(), listOf(article.id))) return@withLock
        }
        if (batch.remainderIds.isNotEmpty()) {
            val notification = articleNotificationBuilder(
                title = strings.newArticlesNotificationTitle,
                body = strings.notificationArticleOverflowBody(batch.remainderIds.size.toString()),
                url = null,
                silent = submitted,
            ).build()
            submit(ArticleNotificationPolicy.OVERFLOW_ID, notification, batch.remainderIds)
        }
    }

    /**
     * Keeps the tray under the soft limit by posting only the newest articles that fit and folding
     * the rest into the overflow notification. Returns null when not even the overflow fits.
     */
    private fun fitToActiveCapacity(
        batch: ArticleNotificationBatch,
        activeKeys: Set<Pair<String?, Int>>,
    ): ArticleNotificationBatch? {
        fun cost(tag: String) = if ((tag to 0) in activeKeys) 0 else 1
        fun articleTag(id: String) = ArticleNotificationPolicy.ARTICLE_ID_PREFIX + id

        val freeSlots = ArticleNotificationPolicy.ANDROID_ACTIVE_SOFT_LIMIT - activeKeys.size
        val overflowCost = cost(ArticleNotificationPolicy.OVERFLOW_ID)
        val fullCost = batch.articles.sumOf { cost(articleTag(it.id)) } +
            if (batch.remainderIds.isNotEmpty()) overflowCost else 0
        if (fullCost <= freeSlots) return batch

        val articleBudget = freeSlots - overflowCost
        if (articleBudget < 0) return null
        var used = 0
        var fittingCount = 0
        for (article in batch.articles) {
            val articleCost = cost(articleTag(article.id))
            if (used + articleCost > articleBudget) break
            used += articleCost
            fittingCount++
        }
        return ArticleNotificationBatch(
            articles = batch.articles.take(fittingCount),
            remainderIds = batch.articles.drop(fittingCount).map { it.id } + batch.remainderIds,
        )
    }

    private suspend fun loadArticleImage(url: String?, allowance: Long): Bitmap? = try {
        imageLoader(url, allowance)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun articleTitle(title: String?, strings: FeedFlowStrings): String =
        title?.trim()?.takeIf { it.isNotEmpty() } ?: strings.newArticlesNotificationTitle

    private fun queryActiveKeys(): Set<Pair<String?, Int>>? = try {
        activeNotificationKeys()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun articleNotificationBuilder(
        title: String,
        body: String,
        url: Uri?,
        silent: Boolean,
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = url
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(silent)
    }

    override fun showNewArticlesNotification(feedSourcesToNotify: List<FeedSourceToNotify>): Boolean {
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            return false
        }

        createNotificationChannel()
        if (!isNotificationChannelEnabled()) {
            return false
        }
        val feedFlowStrings = feedFlowStrings()
        val notificationMode = settingsRepository.getNotificationMode()

        return when (notificationMode) {
            NotificationMode.ARTICLE -> false

            NotificationMode.FEED_SOURCE -> {
                if (feedSourcesToNotify.isEmpty()) {
                    return false
                }
                for (sourceToNotify in feedSourcesToNotify) {
                    showNotification(
                        notificationId = newNotificationId(sourceToNotify.feedSourceId),
                        title = feedFlowStrings.newArticlesNotificationTitle,
                        content = sourceToNotify.feedSourceTitle,
                        url = "feedflow://feedsourcefilter/${sourceToNotify.feedSourceId}",
                    )
                }
                true
            }

            NotificationMode.CATEGORY -> {
                val groupedByCategory = feedSourcesToNotify.groupBy { it.categoryTitle }
                if (groupedByCategory.isEmpty()) {
                    return false
                }
                for ((categoryTitle, sources) in groupedByCategory) {
                    val categoryId = sources.first().categoryId
                    val deeplink = if (categoryId != null) {
                        "feedflow://category/$categoryId"
                    } else {
                        null
                    }

                    val content = if (categoryTitle != null) {
                        feedFlowStrings.notificationCategoryBody(categoryTitle)
                    } else {
                        feedFlowStrings.notificationGroupedBody
                    }
                    showNotification(
                        notificationId = newNotificationId(categoryId ?: categoryTitle ?: "uncategorized"),
                        title = feedFlowStrings.newArticlesNotificationTitle,
                        content = content,
                        url = deeplink,
                    )
                }
                true
            }

            NotificationMode.GROUPED -> {
                if (feedSourcesToNotify.isEmpty()) return false
                showNotification(
                    notificationId = newNotificationId("grouped"),
                    title = feedFlowStrings.newArticlesNotificationTitle,
                    content = feedFlowStrings.notificationGroupedBody,
                    url = null,
                )
                true
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        notificationId: Int,
        title: String,
        content: String,
        url: String?,
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            setAction(Intent.ACTION_VIEW)
            if (url != null) {
                setData(url.toUri())
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        val feedFlowStrings = feedFlowStrings()

        val name = feedFlowStrings.notificationChannelName
        val descriptionText = feedFlowStrings.notificationChannelDescription
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun isNotificationChannelEnabled(): Boolean =
        notificationManager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    private fun feedFlowStrings(): FeedFlowStrings {
        val languageTag = if (settingsRepository.getForceEnglishEnabled()) "en" else Locale.getDefault().toLanguageTag()
        val feedFlowStrings = feedFlowStrings[languageTag] ?: EnFeedFlowStrings
        return feedFlowStrings
    }

    private fun newNotificationId(key: String): Int =
        "$key-${UUID.randomUUID()}".hashCode() and Int.MAX_VALUE

    companion object {
        private const val CHANNEL_ID = "feedflow_new_articles_channel"
    }
}
