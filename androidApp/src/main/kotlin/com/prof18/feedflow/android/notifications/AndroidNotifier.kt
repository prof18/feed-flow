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
import com.prof18.feedflow.android.ui.utils.feedFlowStringsFor
import com.prof18.feedflow.core.model.ArticleNotificationPolicy
import com.prof18.feedflow.core.model.ArticleToNotify
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.NotificationTag
import com.prof18.feedflow.i18n.FeedFlowStrings
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
    private val activeNotifications: () -> List<ActiveNotification> = {
        context.getSystemService(NotificationManager::class.java).activeNotifications.map {
            ActiveNotification(
                tag = it.tag,
                id = it.id,
                isOnNewArticlesChannel = it.notification.channelId == CHANNEL_ID,
                postTime = it.postTime,
            )
        }
    },
    // Callers check areNotificationsEnabled() before posting, as showNewArticlesNotification does.
    @SuppressLint("MissingPermission")
    private val postArticleNotification: (String, Notification) -> Unit = { tag, notification ->
        NotificationManagerCompat.from(context).notify(tag, 0, notification)
    },
    private val cancelNotification: (String?, Int) -> Unit = { tag, id ->
        NotificationManagerCompat.from(context).cancel(tag, id)
    },
) : Notifier, ArticleNotifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    private val articleMutex = Mutex()

    /** Cancels the new-article notifications whose tags [staleTags] reports as read. */
    suspend fun dismissReadNotifications(staleTags: suspend (List<String>) -> List<String>) = articleMutex.withLock {
        val posted = activeNotifications().filter { it.isOnNewArticlesChannel && it.tag != null }
        if (posted.isEmpty()) return@withLock
        val stale = staleTags(posted.mapNotNull { it.tag }).toSet()
        for (notification in posted) {
            if (notification.tag in stale) cancelNotification(notification.tag, notification.id)
        }
    }

    override suspend fun showArticleNotifications(
        pendingArticles: suspend () -> List<ArticleToNotify>,
        acknowledge: suspend (List<String>) -> Unit,
    ) = articleMutex.withLock {
        if (!notificationManager.areNotificationsEnabled()) return@withLock
        createNotificationChannel()
        if (!isNotificationChannelEnabled()) return@withLock
        val active = queryActiveNotifications() ?: return@withLock
        val pending = pendingArticles()
        // Only the newest that fit within the limit can be shown. Left pending, the older ones would
        // push those out on the next refresh, so they are marked notified instead.
        val beyondCapacity = pending.drop(ACTIVE_LIMIT)
        if (beyondCapacity.isNotEmpty()) acknowledge(beyondCapacity.map { it.id })
        val room = makeRoomForBatch(pending.take(ACTIVE_LIMIT), active)
        if (room.articles.isEmpty()) return@withLock

        val strings = feedFlowStrings()
        val postedKeys = mutableSetOf<Pair<String?, Int>>()
        var submitted = false
        var imageElapsed = 0L

        suspend fun submit(tag: String, notification: Notification, ids: List<String>): Boolean {
            currentCoroutineContext().ensureActive()
            val activeKeys = queryActiveNotifications()?.keys() ?: return false
            val key = tag to 0
            // Android frees an evicted slot in its own time, so this batch's evictions don't count.
            if ((activeKeys - room.evicted + postedKeys + key).size > ACTIVE_LIMIT) return false
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

        for (article in room.articles) {
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
            if (!submit(NotificationTag.forArticle(article.id), builder.build(), listOf(article.id))) return@withLock
        }
    }

    /**
     * Android refuses a new notification once an app is at its limit and keeps the older ones, so a
     * full tray would block every later article. The oldest article notifications make room for the
     * newest; their articles stay unread in the timeline.
     */
    private fun makeRoomForBatch(articles: List<ArticleToNotify>, active: List<ActiveNotification>): BatchRoom {
        val activeKeys = active.keys()
        fun cost(id: String) = if ((NotificationTag.forArticle(id) to 0) in activeKeys) 0 else 1

        val needed = articles.sumOf { cost(it.id) }
        val evicted = mutableSetOf<Pair<String?, Int>>()
        val shortfall = needed - (ACTIVE_LIMIT - activeKeys.size)
        if (shortfall > 0) {
            val batchTags = articles.mapTo(mutableSetOf()) { NotificationTag.forArticle(it.id) }
            active.filter { it.isArticle && it.tag !in batchTags }
                .sortedBy { it.postTime }
                .take(shortfall)
                .forEach { oldest ->
                    cancelNotification(oldest.tag, oldest.id)
                    evicted += oldest.key
                }
        }

        val freeSlots = ACTIVE_LIMIT - activeKeys.size + evicted.size
        var used = 0
        val fitting = articles.takeWhile { article ->
            used += cost(article.id)
            used <= freeSlots
        }
        return BatchRoom(fitting, evicted)
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

    private fun queryActiveNotifications(): List<ActiveNotification>? = try {
        activeNotifications()
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
                        tag = NotificationTag.forFeedSource(sourceToNotify.feedSourceId),
                        notificationId = newNotificationId(sourceToNotify.feedSourceId),
                        title = feedFlowStrings.newArticlesNotificationTitle,
                        content = sourceToNotify.feedSourceTitle,
                        url = "feedflow://feedsourcefilter/${sourceToNotify.feedSourceId}",
                    )
                }
                true
            }

            NotificationMode.CATEGORY -> {
                // Grouped by id, not title: two categories can share a title, and the tag a
                // notification carries decides which category's read state cancels it.
                val groupedByCategory = feedSourcesToNotify.groupBy { it.categoryId }
                if (groupedByCategory.isEmpty()) {
                    return false
                }
                for ((categoryId, sources) in groupedByCategory) {
                    val categoryTitle = sources.first().categoryTitle
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
                        tag = NotificationTag.forCategory(categoryId),
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
                    tag = NotificationTag.GROUPED,
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
        tag: String,
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

        notificationManager.notify(tag, notificationId, notification)
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

    private fun feedFlowStrings(): FeedFlowStrings =
        feedFlowStringsFor(forceEnglish = settingsRepository.getForceEnglishEnabled())

    private fun newNotificationId(key: String): Int =
        "$key-${UUID.randomUUID()}".hashCode() and Int.MAX_VALUE

    companion object {
        private const val CHANNEL_ID = "feedflow_new_articles_channel"
        private const val ACTIVE_LIMIT = ArticleNotificationPolicy.ANDROID_ACTIVE_SOFT_LIMIT
    }
}

/** One of the app's active notifications, as far as posting and dismissing need it. */
data class ActiveNotification(
    val tag: String?,
    val id: Int,
    val isOnNewArticlesChannel: Boolean,
    val postTime: Long,
) {
    val key: Pair<String?, Int>
        get() = tag to id

    val isArticle: Boolean
        get() = isOnNewArticlesChannel && tag?.startsWith(ArticleNotificationPolicy.ARTICLE_ID_PREFIX) == true
}

/** The articles of a batch that fit within the limit, and the keys cancelled to fit them. */
private data class BatchRoom(
    val articles: List<ArticleToNotify>,
    val evicted: Set<Pair<String?, Int>>,
)

private fun List<ActiveNotification>.keys(): Set<Pair<String?, Int>> = mapTo(mutableSetOf()) { it.key }
