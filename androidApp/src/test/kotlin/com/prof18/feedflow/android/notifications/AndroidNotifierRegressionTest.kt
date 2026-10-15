package com.prof18.feedflow.android.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.core.model.ArticleNotificationBatch
import com.prof18.feedflow.core.model.ArticleToNotify
import com.prof18.feedflow.core.model.FeedSourceToNotify
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidNotifierRegressionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager by lazy { context.getSystemService(NotificationManager::class.java) }
    private val settings = SettingsRepository(MapSettings())

    @Before
    fun setUp() {
        manager.cancelAll()
        manager.deleteNotificationChannel(CHANNEL_ID)
        shadowOf(manager).setNotificationsEnabled(true)
        settings.setForceEnglishEnabled(true)
    }

    @Test
    fun `active notification query failure defers without image post or acknowledgement`() = runTest {
        var images = 0
        var posts = 0
        val acknowledgements = mutableListOf<List<String>>()
        val notifier = AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { _, _ ->
                images++
                null
            },
            activeNotificationKeys = { error("notification manager unavailable") },
            postArticleNotification = { _, _ -> posts++ },
        )

        notifier.showArticleNotifications({ batch("article") }) { acknowledgements += it }

        assertEquals(0, images)
        assertEquals(0, posts)
        assertTrue(acknowledgements.isEmpty())
    }

    @Test
    fun `disabled channel skips batch preparation and delivery`() = runTest {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "FeedFlow", NotificationManager.IMPORTANCE_NONE),
        )
        var preparations = 0
        var posts = 0
        val acknowledgements = mutableListOf<List<String>>()
        val notifier = AndroidNotifier(
            context = context,
            settingsRepository = settings,
            activeNotificationKeys = { emptySet() },
            postArticleNotification = { _, _ -> posts++ },
        )

        notifier.showArticleNotifications({
            preparations++
            batch("article")
        }) { acknowledgements += it }

        assertEquals(0, preparations)
        assertEquals(0, posts)
        assertTrue(acknowledgements.isEmpty())
    }

    @Test
    fun `capacity growth unions delayed posted keys with active unrelated keys`() = runTest {
        val posts = mutableListOf<String>()
        val acknowledgements = mutableListOf<List<String>>()
        var query = 0
        val unrelated = { count: Int -> (1..count).map { "other-$it" to 0 }.toSet() }
        val notifier = AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { _, _ -> null },
            activeNotificationKeys = {
                query += 1
                when (query) {
                    1, 2 -> unrelated(38)
                    else -> unrelated(39)
                }
            },
            postArticleNotification = { tag, _ -> posts += tag },
        )

        notifier.showArticleNotifications({ batch("first", "second") }) { acknowledgements += it }

        assertEquals(listOf("article-first"), posts)
        assertEquals(listOf(listOf("first")), acknowledgements)
    }

    @Test
    fun `insufficient initial capacity including overflow skips whole batch`() = runTest {
        val imageCalls = mutableListOf<String?>()
        var posts = 0
        val acknowledgements = mutableListOf<List<String>>()
        val notifier = AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { url, _ ->
                imageCalls += url
                null
            },
            activeNotificationKeys = { (1..39).map { "other-$it" to 0 }.toSet() },
            postArticleNotification = { _, _ -> posts++ },
        )

        notifier.showArticleNotifications({ batch("article", remainderIds = listOf("rest")) }) {
            acknowledgements += it
        }

        assertTrue(imageCalls.isEmpty())
        assertEquals(0, posts)
        assertTrue(acknowledgements.isEmpty())
    }

    @Test
    fun `legacy summary modes keep their existing payloads and article mode is not sent by legacy API`() {
        val notifier = AndroidNotifier(context, settings)
        val source = FeedSourceToNotify(
            feedSourceId = "source-id",
            feedSourceTitle = "Source name",
            categoryId = "category-id",
            categoryTitle = "Category name",
        )
        val manager = context.getSystemService(NotificationManager::class.java)

        settings.setNotificationMode(NotificationMode.FEED_SOURCE)
        assertTrue(notifier.showNewArticlesNotification(listOf(source)))
        val sourceEntry = manager.activeNotifications.single()
        assertEquals(
            EnFeedFlowStrings.newArticlesNotificationTitle,
            sourceEntry.notification.extras.getString(Notification.EXTRA_TITLE),
        )
        assertEquals("Source name", sourceEntry.notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(
            "feedflow://feedsourcefilter/source-id",
            shadowOf(sourceEntry.notification.contentIntent).savedIntent.dataString,
        )

        manager.cancelAll()
        settings.setNotificationMode(NotificationMode.CATEGORY)
        assertTrue(notifier.showNewArticlesNotification(listOf(source)))
        val categoryEntry = manager.activeNotifications.single()
        assertEquals(
            EnFeedFlowStrings.notificationCategoryBody("Category name"),
            categoryEntry.notification.extras.getString(Notification.EXTRA_TEXT),
        )
        assertEquals(
            "feedflow://category/category-id",
            shadowOf(categoryEntry.notification.contentIntent).savedIntent.dataString,
        )

        manager.cancelAll()
        settings.setNotificationMode(NotificationMode.GROUPED)
        assertTrue(notifier.showNewArticlesNotification(listOf(source)))
        val groupedEntry = manager.activeNotifications.single()
        assertEquals(
            EnFeedFlowStrings.notificationGroupedBody,
            groupedEntry.notification.extras.getString(Notification.EXTRA_TEXT),
        )
        assertNull(shadowOf(groupedEntry.notification.contentIntent).savedIntent.data)

        manager.cancelAll()
        settings.setNotificationMode(NotificationMode.ARTICLE)
        assertFalse(notifier.showNewArticlesNotification(listOf(source)))
        assertTrue(manager.activeNotifications.isEmpty())
    }

    private fun batch(vararg ids: String, remainderIds: List<String> = emptyList()) = ArticleNotificationBatch(
        articles = ids.map { ArticleToNotify(it, "Title $it", "Source", "https://example.com/image.jpg") },
        remainderIds = remainderIds,
    )

    private companion object {
        const val CHANNEL_ID = "feedflow_new_articles_channel"
    }
}
