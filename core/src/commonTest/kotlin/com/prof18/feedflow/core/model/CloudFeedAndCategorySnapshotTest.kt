package com.prof18.feedflow.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudFeedAndCategorySnapshotTest {

    @Test
    fun `field updates retain unrelated remote fields and additions`() {
        val original = snapshot(
            source = source(title = "Old", category = category("cat", "Tech"), logoUrl = "old-logo"),
            category = category("cat", "Tech"),
        )

        val result = original.applyCloudFeedAndCategoryChanges(
            listOf(
                change(
                    CloudFeedOrCategoryEntity.SOURCE,
                    "source",
                    CloudFeedOrCategoryField.TITLE,
                    "New",
                ),
                change(
                    CloudFeedOrCategoryEntity.CATEGORY,
                    "new-cat",
                    CloudFeedOrCategoryField.EXISTS,
                    "1",
                ),
                change(
                    CloudFeedOrCategoryEntity.CATEGORY,
                    "new-cat",
                    CloudFeedOrCategoryField.TITLE,
                    "News",
                ),
            ),
        )

        val updated = result.sources.single()
        assertEquals("New", updated.title)
        assertEquals("https://example.com/source", updated.url)
        assertEquals("old-logo", updated.logoUrl)
        assertEquals(listOf("cat", "new-cat"), result.categories.map { it.id }.sorted())
    }

    @Test
    fun `deletion is not resurrected by a later rename`() {
        val original = snapshot(source = source(), category = category("cat", "Tech"), category2 = null)

        val result = original.applyCloudFeedAndCategoryChanges(
            listOf(
                change(
                    CloudFeedOrCategoryEntity.SOURCE,
                    "source",
                    CloudFeedOrCategoryField.EXISTS,
                    "0",
                    revision = 1,
                ),
                change(
                    CloudFeedOrCategoryEntity.SOURCE,
                    "source",
                    CloudFeedOrCategoryField.TITLE,
                    "Renamed",
                    revision = 2,
                ),
            ),
        )

        assertEquals(emptyList(), result.sources)
    }

    @Test
    fun `explicit source creation supplies the complete source`() {
        val result = emptySnapshot().applyCloudFeedAndCategoryChanges(
            listOf(
                change(CloudFeedOrCategoryEntity.SOURCE, "source", CloudFeedOrCategoryField.EXISTS, "1"),
                change(
                    CloudFeedOrCategoryEntity.SOURCE,
                    "source",
                    CloudFeedOrCategoryField.URL,
                    "https://example.com/new",
                ),
                change(
                    CloudFeedOrCategoryEntity.SOURCE,
                    "source",
                    CloudFeedOrCategoryField.TITLE,
                    "New",
                ),
                change(CloudFeedOrCategoryEntity.SOURCE, "source", CloudFeedOrCategoryField.LOGO, "logo"),
            ),
        )

        assertEquals(
            ParsedFeedSource(
                id = "source",
                url = "https://example.com/new",
                title = "New",
                category = null,
                logoUrl = "logo",
                websiteUrl = null,
            ),
            result.sources.single(),
        )
    }

    @Test
    fun `category deletion uncategorizes existing sources`() {
        val original = snapshot(source = source(category = category("cat", "Tech")), category = category("cat", "Tech"))

        val result = original.applyCloudFeedAndCategoryChanges(
            listOf(
                change(CloudFeedOrCategoryEntity.CATEGORY, "cat", CloudFeedOrCategoryField.EXISTS, "0"),
            ),
        )

        assertEquals(null, result.sources.single().category)
        assertEquals(emptyList(), result.categories)
    }

    @Test
    fun `source category can be explicitly cleared`() {
        val original = snapshot(source = source(category = category("cat", "Tech")), category = category("cat", "Tech"))

        val result = original.applyCloudFeedAndCategoryChanges(
            listOf(
                change(CloudFeedOrCategoryEntity.SOURCE, "source", CloudFeedOrCategoryField.CATEGORY, null),
            ),
        )

        assertEquals(null, result.sources.single().category)
    }

    @Test
    fun `empty collections remain empty`() {
        assertEquals(emptySnapshot(), emptySnapshot().applyCloudFeedAndCategoryChanges(emptyList()))
    }

    @Test
    fun `conflicting category names are rejected`() {
        val original = snapshot(category = category("cat", "Tech"))

        assertFailsWith<IllegalStateException> {
            original.applyCloudFeedAndCategoryChanges(
                listOf(
                    change(
                        CloudFeedOrCategoryEntity.CATEGORY,
                        "other",
                        CloudFeedOrCategoryField.EXISTS,
                        "1",
                    ),
                    change(
                        CloudFeedOrCategoryEntity.CATEGORY,
                        "other",
                        CloudFeedOrCategoryField.TITLE,
                        "Tech",
                    ),
                ),
            )
        }
    }

    private fun change(
        entity: CloudFeedOrCategoryEntity,
        id: String,
        field: CloudFeedOrCategoryField,
        value: String?,
        revision: Long = 1,
    ) = CloudPendingFeedOrCategoryChange(entity, id, field, value, revision)

    private fun snapshot(
        source: ParsedFeedSource? = null,
        category: FeedSourceCategory? = null,
        category2: FeedSourceCategory? = null,
    ) = CloudFeedAndCategorySnapshot(
        sources = listOfNotNull(source),
        categories = listOfNotNull(category, category2),
    )

    private fun emptySnapshot() = CloudFeedAndCategorySnapshot(emptyList(), emptyList())

    private fun source(
        title: String = "Source",
        category: FeedSourceCategory? = null,
        logoUrl: String? = null,
    ) = ParsedFeedSource(
        id = "source",
        url = "https://example.com/source",
        title = title,
        category = category,
        logoUrl = logoUrl,
        websiteUrl = null,
    )

    private fun category(id: String, title: String) = FeedSourceCategory(id = id, title = title)
}
