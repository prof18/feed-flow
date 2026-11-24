package com.prof18.feedflow.android.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.core.model.ArticleToNotify
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Assertions compare against English strings, so keep them independent of the host locale.
    private val settings = SettingsRepository(MapSettings()).also { it.setForceEnglishEnabled(true) }
    private val posts = mutableListOf<Pair<String, Notification>>()
    private val acks = mutableListOf<List<String>>()
    private val cancelled = mutableListOf<Pair<String?, Int>>()
    private var active = listOf<ActiveNotification>()
    private var time = 0L
    private var imageCalls = 0
    private var image: Bitmap? = null
    private var postAction: (String, Notification) -> Unit = { _, _ -> }

    // Android frees a cancelled slot in its own time; turn this off to keep the tray listing it.
    private var freeSlotOnCancel = true
    private val notifier by lazy {
        AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { _, _ ->
                imageCalls++
                image
            },
            monotonicMillis = { time },
            activeNotifications = { active },
            postArticleNotification = { tag, notification ->
                postAction(tag, notification)
                posts += tag to notification
                active = active.filterNot { it.tag == tag } + newArticle(tag, postTime = active.size.toLong() + 1)
            },
            cancelNotification = { tag, id ->
                cancelled += tag to id
                if (freeSlotOnCancel) active = active.filterNot { it.key == (tag to id) }
            },
        )
    }

    @Test
    fun `article payload uses distinct stable full IDs and encoded tap data`() = runTest {
        val articles = articles("Aa", "BB", "id/with spaces")
        send(articles)
        assertEquals(listOf("article-Aa", "article-BB", "article-id/with spaces"), posts.map { it.first })
        assertEquals(articles.map { listOf(it.id) }, acks)
        val notification = posts.last().second
        assertEquals("Title id/with spaces", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Source", notification.extras.getString(Notification.EXTRA_TEXT))
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals("feedflow://feed/id%2Fwith%20spaces", intent.dataString)
        assertEquals("id/with spaces", intent.data!!.lastPathSegment)
        assertTrue(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        send(articles)
        assertEquals(posts.take(3).map { it.first }, posts.drop(3).map { it.first })
    }

    @Test
    fun `blank titles fallback and images use big picture while missing images stay text`() = runTest {
        image = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        send(listOf(ArticleToNotify("a", "  ", "Source", "https://example.com/a")))
        assertEquals(
            EnFeedFlowStrings.newArticlesNotificationTitle,
            posts.single().second.extras.getString(Notification.EXTRA_TITLE),
        )
        assertNotNull(posts.single().second.extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE))
        image = null
        send(articles("b"))
        assertNull(posts.last().second.extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE))
    }

    @Test
    fun `every article in a large batch gets its own notification and no summary`() = runTest {
        val ids = (1..30).map { "item-$it" }
        send(articles(*ids.toTypedArray()))
        assertEquals(ids.map { "article-$it" }, posts.map { it.first })
        assertEquals(ids.map { listOf(it) }, acks)
        assertTrue(posts.none { (_, notification) -> notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 })
        val intent = shadowOf(posts.first().second.contentIntent).savedIntent
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, intent.flags)
    }

    @Test
    fun `articles beyond what the tray can hold are marked notified instead of left pending`() = runTest {
        val ids = (1..45).map { "item-$it" }
        send(articles(*ids.toTypedArray()))
        assertEquals(ids.take(40).map { "article-$it" }, posts.map { it.first })
        // Left pending, the five oldest would push the newest notifications out on the next refresh.
        assertEquals(ids.drop(40), acks.first())
        assertEquals(ids.take(40).map { listOf(it) }, acks.drop(1))
    }

    @Test
    fun `failed post remains pending and first successful submission owns alert`() = runTest {
        postAction = { tag, _ -> if (tag == "article-a") error("post failed") }
        send(articles("a", "b", "c"))
        assertEquals(listOf(listOf("b"), listOf("c")), acks)
        assertFalse(posts[0].second.group == "silent")
        assertEquals("silent", posts[1].second.group)
    }

    @Test
    fun `acknowledgement failure and cancellation stop further submissions`() = runTest {
        val ackFailure = runCatching {
            notifier.showArticleNotifications({ articles("a", "b") }) { error("database failed") }
        }.exceptionOrNull()
        assertEquals("database failed", ackFailure?.message)
        assertEquals(1, posts.size)
        posts.clear()
        active = emptyList()
        postAction = { _, _ -> throw CancellationException("cancelled") }
        assertTrue(runCatching { send(articles("a", "b")) }.exceptionOrNull() is CancellationException)
        assertTrue(acks.isEmpty())
    }

    @Test
    fun `empty denied and insufficient capacity batches perform no image post or acknowledgement`() = runTest {
        send(emptyList())
        active = others(40)
        send(articles("a"))
        assertTrue(posts.isEmpty())
        assertEquals(0, imageCalls)
        assertTrue(acks.isEmpty())
        active = emptyList()
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        send(articles("a"))
        assertTrue(posts.isEmpty())
    }

    @Test
    fun `capacity exact fit and stable updates need no extra slot`() = runTest {
        active = others(38)
        send(articles("a", "b"))
        assertEquals(2, posts.size)
        posts.clear()
        active = others(39) + newArticle("article-a")
        send(articles("a"))
        assertEquals(1, posts.size)
    }

    @Test
    fun `partial capacity posts newest articles that fit and keeps the rest pending`() = runTest {
        active = others(35)
        send(articles("a", "b", "c", "d", "e", "f", "g"))
        assertEquals(
            listOf("article-a", "article-b", "article-c", "article-d", "article-e"),
            posts.map { it.first },
        )
        assertEquals(listOf(listOf("a"), listOf("b"), listOf("c"), listOf("d"), listOf("e")), acks)
    }

    @Test
    fun `mid batch capacity growth stops unsent work and later dismissal permits retry`() = runTest {
        postAction = { _, _ -> active = others(40) }
        send(articles("a", "b"))
        assertEquals(listOf(listOf("a")), acks)
        active = emptyList()
        postAction = { _, _ -> }
        send(articles("b"))
        assertEquals(listOf(listOf("a"), listOf("b")), acks)
    }

    @Test
    fun `a full tray makes room by dropping the oldest article notifications`() = runTest {
        active = oldArticles(40)
        send(articles("new"))
        assertEquals(listOf<Pair<String?, Int>>("article-old-1" to 0), cancelled)
        assertEquals(listOf("article-new"), posts.map { it.first })
        assertEquals(listOf(listOf("new")), acks)
    }

    @Test
    fun `a slot freed by eviction is not counted again while the tray still lists it`() = runTest {
        active = oldArticles(40)
        freeSlotOnCancel = false
        send(articles("new"))
        assertEquals(listOf<Pair<String?, Int>>("article-old-1" to 0), cancelled)
        assertEquals(listOf("article-new"), posts.map { it.first })
        assertEquals(listOf(listOf("new")), acks)
    }

    @Test
    fun `a tray full of other notifications is left alone and nothing is posted`() = runTest {
        active = others(40)
        send(articles("new"))
        assertTrue(cancelled.isEmpty())
        assertTrue(posts.isEmpty())
        assertTrue(acks.isEmpty())
    }

    @Test
    fun `image budget only counts image preparation and skips later work after exhaustion`() = runTest {
        val allowances = mutableListOf<Long>()
        val sender = AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { _, allowance ->
                allowances += allowance
                time += 1000
                null
            },
            monotonicMillis = { time },
            activeNotifications = { emptyList() },
            postArticleNotification = { tag, notification ->
                posts += tag to notification
                time += 100_000
            },
        )
        sender.showArticleNotifications({ articles(*(1..7).map { "$it" }.toTypedArray()) }) { acks += it }
        assertEquals(List(5) { 1000L }, allowances)
        assertEquals(7, posts.size)
    }

    @Test
    fun `second caller prepares only after first acknowledgement and observes newly pending rows`() = runTest {
        val posted = CompletableDeferred<Unit>()
        val releaseAck = CompletableDeferred<Unit>()
        val pending = mutableListOf("a")
        var preparations = 0
        val sender = AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { _, _ -> null },
            activeNotifications = { emptyList() },
            postArticleNotification = { tag, notification ->
                posts += tag to notification
                posted.complete(Unit)
            },
        )
        val prepare: suspend () -> List<ArticleToNotify> = {
            preparations++
            articles(*pending.toTypedArray())
        }
        val first = launch {
            sender.showArticleNotifications(prepare) { ids ->
                releaseAck.await()
                pending.removeAll(ids)
                pending += "new"
            }
        }
        posted.await()
        val second = launch {
            sender.showArticleNotifications(prepare) { pending.removeAll(it) }
        }
        runCurrent()
        assertEquals(1, preparations)
        releaseAck.complete(Unit)
        first.join()
        second.join()
        assertEquals(listOf("article-a", "article-new"), posts.map { it.first })
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `cancelling waiting caller never prepares posts or acknowledges`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = launch {
            notifier.showArticleNotifications({ articles("a") }) {
                entered.complete(Unit)
                release.await()
            }
        }
        entered.await()
        val waiting = launch {
            notifier.showArticleNotifications({ error("must not prepare") }) { error("must not ack") }
        }
        runCurrent()
        waiting.cancelAndJoin()
        release.complete(Unit)
        first.join()
        assertEquals(1, posts.size)
    }

    @Test
    fun `cancellation during active notification query propagates`() = runTest {
        val sender = AndroidNotifier(
            context = context,
            settingsRepository = settings,
            activeNotifications = { throw CancellationException("cancel query") },
        )
        val failure = runCatching {
            sender.showArticleNotifications({ articles("a") }) { error("must not acknowledge") }
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }

    private suspend fun send(articles: List<ArticleToNotify>) {
        notifier.showArticleNotifications({ articles }) { acks += it }
    }

    private fun articles(vararg ids: String) =
        ids.map { ArticleToNotify(it, "Title $it", "Source", "https://example.com/image") }

    private fun newArticle(tag: String, postTime: Long = 0) =
        ActiveNotification(tag = tag, id = 0, isOnNewArticlesChannel = true, postTime = postTime)

    // Notifications the app posted elsewhere: they take tray slots but are never evicted.
    private fun others(count: Int) =
        (1..count).map { ActiveNotification(tag = "other-$it", id = 0, isOnNewArticlesChannel = false, postTime = 0) }

    private fun oldArticles(count: Int) = (1..count).map { newArticle("article-old-$it", postTime = it.toLong()) }
}
