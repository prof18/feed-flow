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
import co.touchlab.kermit.Logger
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.R
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.domain.notification.Notifier
import java.util.*

class AndroidNotifier(private val context: Context) : Notifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    @SuppressLint("MissingPermission")
    override fun showNewArticlesNotification(feedSourcesToNotify: List<FeedSourceToNotify>) {
        createNotificationChannel()
        val feedFlowStrings = feedFlowStrings()

        for ((index, sourceToNotify) in feedSourcesToNotify.withIndex()) {
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                setAction(Intent.ACTION_VIEW)
                val deeplink = "feedflow://feedsourcefilter/${sourceToNotify.feedSourceId}"
                Logger.d { "Setting deeplink: $deeplink" }
                setData(deeplink.toUri())
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val textContent = sourceToNotify.feedSourceTitle
            val textTitle = feedFlowStrings.newArticlesNotificationTitle

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(BASE_NOTIFICATION_ID + index, notification)
        }
    }

    private fun createNotificationChannel() {
        val feedFlowStrings = feedFlowStrings()

        val name = feedFlowStrings.notificationChannelName
        val descriptionText = feedFlowStrings.notificationChannelDescription
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun feedFlowStrings(): FeedFlowStrings {
        val languageTag = Locale.getDefault().toLanguageTag()
        val feedFlowStrings = feedFlowStrings[languageTag] ?: EnFeedFlowStrings
        return feedFlowStrings
    }

    companion object {
        private const val CHANNEL_ID = "feedflow_new_articles_channel"
        private const val BASE_NOTIFICATION_ID = 1001
    }
}
