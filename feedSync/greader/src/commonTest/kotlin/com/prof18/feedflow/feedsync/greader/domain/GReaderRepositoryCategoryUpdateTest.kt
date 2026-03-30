package com.prof18.feedflow.feedsync.greader.domain

import com.prof18.feedflow.core.model.FeedSourceCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class GReaderRepositoryCategoryUpdateTest {

    @Test
    fun `buildCategoryUpdate returns no category mutation when category id is unchanged`() {
        val category = FeedSourceCategory(
            id = "user/-/label/Tech",
            title = "Tech",
        )

        val result = buildCategoryUpdate(
            originalCategory = category,
            newCategory = category.copy(title = "Technology"),
        )

        assertEquals(CategoryUpdate(), result)
    }

    @Test
    fun `buildCategoryUpdate returns add and remove ids when category changes`() {
        val result = buildCategoryUpdate(
            originalCategory = FeedSourceCategory(
                id = "user/-/label/News",
                title = "News",
            ),
            newCategory = FeedSourceCategory(
                id = "user/-/label/Tech",
                title = "Tech",
            ),
        )

        assertEquals(
            CategoryUpdate(
                addCategoryId = "user/-/label/Tech",
                removeCategoryId = "user/-/label/News",
            ),
            result,
        )
    }

    @Test
    fun `buildCategoryUpdate returns add id only when moving from uncategorized`() {
        val result = buildCategoryUpdate(
            originalCategory = null,
            newCategory = FeedSourceCategory(
                id = "user/-/label/Tech",
                title = "Tech",
            ),
        )

        assertEquals(
            CategoryUpdate(addCategoryId = "user/-/label/Tech"),
            result,
        )
    }

    @Test
    fun `buildCategoryUpdate returns remove id only when moving to uncategorized`() {
        val result = buildCategoryUpdate(
            originalCategory = FeedSourceCategory(
                id = "user/-/label/Tech",
                title = "Tech",
            ),
            newCategory = null,
        )

        assertEquals(
            CategoryUpdate(removeCategoryId = "user/-/label/Tech"),
            result,
        )
    }
}
