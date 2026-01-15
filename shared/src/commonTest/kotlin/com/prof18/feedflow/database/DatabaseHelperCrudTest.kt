package com.prof18.feedflow.database

import com.prof18.feedflow.shared.test.DatabaseTestBase
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.sampleValue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DatabaseHelperCrudTest : DatabaseTestBase() {

    @Test
    fun `should insert and retrieve feed sources`() = runTest {
        checkAll(10, FeedSourceGenerator.feedSourceArb) { feedSource ->
            database.insertFeedSourceWithCategory(feedSource)

            val retrieved = database.getFeedSource(feedSource.id)

            retrieved.shouldNotBeNull()
            retrieved.id shouldBe feedSource.id
            retrieved.title shouldBe feedSource.title
            retrieved.url shouldBe feedSource.url
            retrieved.logoUrl shouldBe feedSource.logoUrl
            retrieved.websiteUrl shouldBe feedSource.websiteUrl
            retrieved.category?.id shouldBe feedSource.category?.id
        }
    }

    @Test
    fun `should update existing feed source`() = runTest {
        val original = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(original)

        val updated = original.copy(
            title = "Updated Title",
            url = "https://example.com/updated.xml",
        )
        database.updateFeedSource(updated)

        val retrieved = database.getFeedSource(original.id)
        retrieved.shouldNotBeNull()
        retrieved.title shouldBe "Updated Title"
        retrieved.url shouldBe "https://example.com/updated.xml"
    }

    @Test
    fun `should delete feed source`() = runTest {
        val feedSource = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertFeedSourceWithCategory(feedSource)

        database.deleteFeedSource(feedSource.id)

        database.getFeedSource(feedSource.id).shouldBeNull()
        database.getFeedSources().shouldBeEmpty()
    }

    @Test
    fun `should retrieve all feed sources`() = runTest {
        val sources = List(5) { FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null) }
        sources.forEach { database.insertFeedSourceWithCategory(it) }

        val allSources = database.getFeedSources()
        allSources.shouldHaveSize(5)
        allSources.map { it.id }.toSet() shouldBe sources.map { it.id }.toSet()
    }
}
