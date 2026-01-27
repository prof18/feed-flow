package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.feedsync.feedbin.data.dto.TaggingDTO
import kotlin.test.Test
import kotlin.test.assertEquals

class TaggingDTOMapperTest {

    @Test
    fun `toFeedSourceCategory maps name to id and title`() {
        val tagging = TaggingDTO(
            id = 1,
            feedId = 42,
            name = "Tech",
        )

        val result = tagging.toFeedSourceCategory()

        assertEquals("Tech", result.id)
        assertEquals("Tech", result.title)
    }
}
