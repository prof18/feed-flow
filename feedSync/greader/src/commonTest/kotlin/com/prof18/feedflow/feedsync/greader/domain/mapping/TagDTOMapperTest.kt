package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.feedsync.greader.data.dto.TagDTO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagDTOMapperTest {

    @Test
    fun `toFeedSourceCategory returns FeedSourceCategory with correct values when label is present`() {
        val tagDTO = TagDTO(
            id = "user/-/label/Tech",
            label = "Tech",
        )

        val result = tagDTO.toFeedSourceCategory()

        assertEquals("user/-/label/Tech", result?.id)
        assertEquals("Tech", result?.title)
    }

    @Test
    fun `toFeedSourceCategory returns null when label is null`() {
        val tagDTO = TagDTO(
            id = "user/-/label/Unknown",
            label = null,
        )

        val result = tagDTO.toFeedSourceCategory()

        assertNull(result)
    }

    @Test
    fun `toFeedSourceCategory handles empty label correctly`() {
        val tagDTO = TagDTO(
            id = "user/-/label/Empty",
            label = "",
        )

        val result = tagDTO.toFeedSourceCategory()

        assertEquals("user/-/label/Empty", result?.id)
        assertEquals("", result?.title)
    }
}
