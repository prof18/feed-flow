package com.prof18.feedflow.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetContentFilterTest {
    @Test
    fun `all variants round trip`() {
        val filters = listOf(
            WidgetContentFilter.Timeline,
            WidgetContentFilter.Bookmarks,
            WidgetContentFilter.Category("cat"),
            WidgetContentFilter.Source("src"),
        )

        filters.forEach { filter ->
            assertEquals(filter, WidgetContentFilter.deserialize(filter.serialize()))
        }
    }

    @Test
    fun `ids containing separators survive`() {
        val filter = WidgetContentFilter.Category("a:b:c")
        assertEquals(filter, WidgetContentFilter.deserialize(filter.serialize()))
    }

    @Test
    fun `invalid values default to timeline`() {
        assertEquals(WidgetContentFilter.Timeline, WidgetContentFilter.deserialize(null))
        assertEquals(WidgetContentFilter.Timeline, WidgetContentFilter.deserialize("garbage"))
        assertEquals(WidgetContentFilter.Timeline, WidgetContentFilter.deserialize("category:"))
        assertEquals(WidgetContentFilter.Timeline, WidgetContentFilter.deserialize("source:"))
    }
}
