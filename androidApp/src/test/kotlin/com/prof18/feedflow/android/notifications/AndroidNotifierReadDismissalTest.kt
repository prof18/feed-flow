package com.prof18.feedflow.android.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.core.model.ArticleToNotify
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.shared.data.SettingsRepository
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidNotifierReadDismissalTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val systemNotificationManager = context.getSystemService(NotificationManager::class.java)
    private val settings = SettingsRepository(MapSettings()).also { it.setForceEnglishEnabled(true) }
    private val notifier by lazy {
        AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { _, _ -> null },
        )
    }

    @Test
    fun `only the notifications whose tags come back stale are removed`() = runTest {
        settings.setNotificationMode(NotificationMode.FEED_SOURCE)
        notifier.showNewArticlesNotification(listOf(source("feed-a"), source("feed-b")))
        notifier.showArticleNotifications({ listOf(article("read"), article("unread")) }) {}
        var asked = emptyList<String>()

        notifier.dismissReadNotifications { tags ->
            asked = tags.sorted()
            listOf("feed-source-feed-a", "article-read")
        }

        assertEquals(listOf("article-read", "article-unread", "feed-source-feed-a", "feed-source-feed-b"), asked)
        assertEquals(listOf("article-unread", "feed-source-feed-b"), activeTags())
    }

    @Test
    fun `untagged and other-channel notifications are neither asked about nor removed`() = runTest {
        notifier.showArticleNotifications({ listOf(article("a")) }) {}
        // An untagged one on the new-articles channel, as older versions posted them.
        systemNotificationManager.notify(42, notification(channelId = "feedflow_new_articles_channel"))
        systemNotificationManager.notify("feed-source-x", 7, notification(channelId = "audio_playback"))
        var asked = emptyList<String>()

        notifier.dismissReadNotifications { tags ->
            asked = tags
            tags
        }

        assertEquals(listOf("article-a"), asked)
        assertEquals(setOf(7, 42), systemNotificationManager.activeNotifications.map { it.id }.toSet())
    }

    @Test
    fun `an empty tray asks nothing`() = runTest {
        notifier.dismissReadNotifications { error("asked with an empty tray") }
    }

    private fun notification(channelId: String) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("New articles")
        .build()

    private fun activeTags(): List<String?> =
        systemNotificationManager.activeNotifications.map { it.tag }.sortedBy { it.orEmpty() }

    private fun source(id: String) = FeedSourceToNotify(
        feedSourceId = id,
        feedSourceTitle = "Feed $id",
        categoryId = null,
        categoryTitle = null,
    )

    private fun article(id: String) = ArticleToNotify(
        id = id,
        title = "Title $id",
        feedSourceTitle = "Source",
        imageUrl = null,
    )
}
