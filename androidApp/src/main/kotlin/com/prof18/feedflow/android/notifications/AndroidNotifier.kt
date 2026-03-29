package com.prof18.feedflow.android.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.R
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.notification.Notifier
import java.util.*

class AndroidNotifier(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : Notifier {

    private val notificationManager = NotificationManagerCompat.from(context)

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
        val languageTag = Locale.getDefault().toLanguageTag()
        val feedFlowStrings = feedFlowStrings[languageTag] ?: EnFeedFlowStrings
        return feedFlowStrings
    }

    private fun newNotificationId(key: String): Int =
        "$key-${UUID.randomUUID()}".hashCode() and Int.MAX_VALUE

    companion object {
        private const val CHANNEL_ID = "feedflow_new_articles_channel"
    }
}
