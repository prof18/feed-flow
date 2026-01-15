package com.prof18.feedflow.database

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.sampleValue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock

class DatabaseHelperSearchTest : DatabaseTestBase() {

    @Test
    fun `should find items by title`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val items = listOf(
            buildFeedItem("title-1", "Kotlin News", now, source),
            buildFeedItem("title-2", "Java Update", now - 1000L, source),
            buildFeedItem("title-3", "Kotlin 2.0 Released", now - 2000L, source),
        )
        database.insertFeedItems(items, lastSyncTimestamp = now)

        val results = database.search("Kotlin").first()
        results.size shouldBe 2
        results.map { it.title } shouldContain "Kotlin News"
        results.map { it.title } shouldContain "Kotlin 2.0 Released"
    }

    @Test
    fun `should find items by subtitle`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val item = buildFeedItem(
            id = "subtitle-1",
            title = "Article",
            subtitle = "This is about Android development",
            pubDateMillis = now,
            source = source,
        )
        database.insertFeedItems(listOf(item), lastSyncTimestamp = now)

        val results = database.search("Android").first()
        results.size shouldBe 1
        results.first().url_hash shouldBe "subtitle-1"
    }

    @Test
    fun `should be case insensitive`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val item = buildFeedItem("case-1", "Kotlin News", now, source)
        database.insertFeedItems(listOf(item), lastSyncTimestamp = now)

        val results = database.search("kotlin").first()
        results.size shouldBe 1
        results.first().url_hash shouldBe "case-1"
    }

    @Test
    fun `should respect read filter during search`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(source)

        val now = Clock.System.now().toEpochMilliseconds()
        val readItem = buildFeedItem("read-1", "Read Kotlin Article", now, source)
        val unreadItem = buildFeedItem("unread-1", "Unread Kotlin Article", now - 1000L, source)
        database.insertFeedItems(listOf(readItem, unreadItem), lastSyncTimestamp = now)
        database.updateReadStatus(FeedItemId(readItem.id), isRead = true)

        val results = database.search("Kotlin", feedFilter = FeedFilter.Read).first()
        results.size shouldBe 1
        results.first().url_hash shouldBe "read-1"
    }
}
