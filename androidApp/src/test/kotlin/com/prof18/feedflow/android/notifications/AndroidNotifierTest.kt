package com.prof18.feedflow.android.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.core.model.ArticleNotificationBatch
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
    private val settings = SettingsRepository(MapSettings())
    private val posts = mutableListOf<Pair<String, Notification>>()
    private val acks = mutableListOf<List<String>>()
    private var active = emptySet<Pair<String?, Int>>()
    private var time = 0L
    private var imageCalls = 0
    private var image: Bitmap? = null
    private var postAction: (String, Notification) -> Unit = { _, _ -> }
    private val notifier by lazy {
        AndroidNotifier(
            context = context,
            settingsRepository = settings,
            imageLoader = { _, _ ->
                imageCalls++
                image
            },
            monotonicMillis = { time },
            activeNotificationKeys = { active },
            postArticleNotification = { tag, notification ->
                postAction(tag, notification)
                posts += tag to notification
            },
        )
    }

    @Test
    fun `article payload uses distinct stable full IDs and encoded tap data`() = runTest {
        val batch = batch("Aa", "BB", "id/with spaces")
        send(batch)
        assertEquals(listOf("article-Aa", "article-BB", "article-id/with spaces"), posts.map { it.first })
        assertEquals(batch.articles.map { listOf(it.id) }, acks)
        val notification = posts.last().second
        assertEquals("Title id/with spaces", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Source", notification.extras.getString(Notification.EXTRA_TEXT))
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals("feedflow://feed/id%2Fwith%20spaces", intent.dataString)
        assertEquals("id/with spaces", intent.data!!.lastPathSegment)
        assertTrue(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        send(batch)
        assertEquals(posts.take(3).map { it.first }, posts.drop(3).map { it.first })
    }

    @Test
    fun `blank titles fallback and images use big picture while missing images stay text`() = runTest {
        image = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        send(
            ArticleNotificationBatch(
                listOf(ArticleToNotify("a", "  ", "Source", "https://example.com/a")),
                emptyList(),
            ),
        )
        assertEquals(
            EnFeedFlowStrings.newArticlesNotificationTitle,
            posts.single().second.extras.getString(Notification.EXTRA_TITLE),
        )
        assertNotNull(posts.single().second.extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE))
        image = null
        send(batch("b"))
        assertNull(posts.last().second.extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE))
    }

    @Test
    fun `overflow acknowledges only remainder and retains combined task flags without URL`() = runTest {
        send(ArticleNotificationBatch(batch("a").articles, listOf("r1", "r2")))
        assertEquals(listOf(listOf("a"), listOf("r1", "r2")), acks)
        val (tag, notification) = posts.last()
        assertEquals("article-overflow", tag)
        assertEquals(
            EnFeedFlowStrings.notificationArticleOverflowBody("2"),
            notification.extras.getString(Notification.EXTRA_TEXT),
        )
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertNull(intent.data)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, intent.flags)
    }

    @Test
    fun `failed post remains pending and first successful submission owns alert`() = runTest {
        postAction = { tag, _ -> if (tag == "article-a") error("post failed") }
        send(batch("a", "b", "c"))
        assertEquals(listOf(listOf("b"), listOf("c")), acks)
        assertFalse(posts[0].second.group == "silent")
        assertEquals("silent", posts[1].second.group)
    }

    @Test
    fun `acknowledgement failure and cancellation stop further submissions`() = runTest {
        val ackFailure = runCatching {
            notifier.showArticleNotifications({ batch("a", "b") }) { error("database failed") }
        }.exceptionOrNull()
        assertEquals("database failed", ackFailure?.message)
        assertEquals(1, posts.size)
        posts.clear()
        postAction = { _, _ -> throw CancellationException("cancelled") }
        assertTrue(runCatching { send(batch("a", "b")) }.exceptionOrNull() is CancellationException)
        assertTrue(acks.isEmpty())
    }

    @Test
    fun `empty denied and insufficient capacity batches perform no image post or acknowledgement`() = runTest {
        send(ArticleNotificationBatch(emptyList(), emptyList()))
        active = (1..40).map { "old-$it" to 0 }.toSet()
        send(batch("a"))
        assertTrue(posts.isEmpty())
        assertEquals(0, imageCalls)
        assertTrue(acks.isEmpty())
        active = emptySet()
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        send(batch("a"))
        assertTrue(posts.isEmpty())
    }

    @Test
    fun `capacity exact fit counts overflow and stable updates need no extra slot`() = runTest {
        active = (1..38).map { "old-$it" to 0 }.toSet()
        send(ArticleNotificationBatch(batch("a").articles, listOf("r")))
        assertEquals(2, posts.size)
        posts.clear()
        active = (1..39).map { "old-$it" to 0 }.toSet() + ("article-a" to 0)
        send(batch("a"))
        assertEquals(1, posts.size)
    }

    @Test
    fun `partial capacity posts newest articles that fit and overflow absorbs the rest`() = runTest {
        active = (1..35).map { "old-$it" to 0 }.toSet()
        send(ArticleNotificationBatch(batch("a", "b", "c", "d", "e", "f").articles, listOf("r")))
        assertEquals(
            listOf("article-a", "article-b", "article-c", "article-d", "article-overflow"),
            posts.map { it.first },
        )
        assertEquals(listOf(listOf("a"), listOf("b"), listOf("c"), listOf("d"), listOf("e", "f", "r")), acks)
        assertEquals(
            EnFeedFlowStrings.notificationArticleOverflowBody("3"),
            posts.last().second.extras.getString(Notification.EXTRA_TEXT),
        )
    }

    @Test
    fun `full tray from an earlier batch still delivers the next batch through the existing overflow`() = runTest {
        active = (1..20).map { "article-old-$it" to 0 }.toSet() + ("article-overflow" to 0)
        val ids = (1..20).map { "new-$it" }
        send(ArticleNotificationBatch(batch(*ids.toTypedArray()).articles, listOf("r")))
        assertEquals(ids.take(19).map { "article-$it" } + "article-overflow", posts.map { it.first })
        assertEquals(listOf("new-20", "r"), acks.last())
    }

    @Test
    fun `mid batch capacity growth stops unsent work and later dismissal permits retry`() = runTest {
        postAction = { _, _ -> active = (1..40).map { "other-$it" to 0 }.toSet() }
        send(batch("a", "b"))
        assertEquals(listOf(listOf("a")), acks)
        active = emptySet()
        postAction = { _, _ -> }
        send(batch("b"))
        assertEquals(listOf(listOf("a"), listOf("b")), acks)
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
            activeNotificationKeys = { emptySet() },
            postArticleNotification = { tag, notification ->
                posts += tag to notification
                time += 100_000
            },
        )
        sender.showArticleNotifications({
            batch(*(1..7).map { "$it" }.toTypedArray())
        }) { acks += it }
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
            activeNotificationKeys = { emptySet() },
            postArticleNotification = { tag, notification ->
                posts += tag to notification
                posted.complete(Unit)
            },
        )
        val prepare: suspend () -> ArticleNotificationBatch = {
            preparations++
            batch(*pending.toTypedArray())
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
            notifier.showArticleNotifications({ batch("a") }) {
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
            activeNotificationKeys = { throw CancellationException("cancel query") },
        )
        val failure = runCatching {
            sender.showArticleNotifications({ batch("a") }) { error("must not acknowledge") }
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }

    private suspend fun send(batch: ArticleNotificationBatch) {
        notifier.showArticleNotifications({ batch }) { acks += it }
    }

    private fun batch(vararg ids: String) = ArticleNotificationBatch(
        articles = ids.map { ArticleToNotify(it, "Title $it", "Source", "https://example.com/image") },
        remainderIds = emptyList(),
    )
}
