package com.prof18.feedflow.core.model

sealed class WidgetContentFilter {
    data object Timeline : WidgetContentFilter()
    data object Bookmarks : WidgetContentFilter()
    data class Category(val categoryId: String) : WidgetContentFilter()
    data class Source(val feedSourceId: String) : WidgetContentFilter()

    fun serialize(): String = when (this) {
        Timeline -> TYPE_TIMELINE
        Bookmarks -> TYPE_BOOKMARKS
        is Category -> "$TYPE_CATEGORY$SEPARATOR$categoryId"
        is Source -> "$TYPE_SOURCE$SEPARATOR$feedSourceId"
    }

    companion object {
        private const val SEPARATOR = ":"
        private const val TYPE_TIMELINE = "timeline"
        private const val TYPE_BOOKMARKS = "bookmarks"
        private const val TYPE_CATEGORY = "category"
        private const val TYPE_SOURCE = "source"

        fun deserialize(value: String?): WidgetContentFilter = when {
            value == null -> Timeline
            value == TYPE_TIMELINE -> Timeline
            value == TYPE_BOOKMARKS -> Bookmarks
            value.startsWith("$TYPE_CATEGORY$SEPARATOR") ->
                value.substringAfter(SEPARATOR)
                    .takeIf(String::isNotEmpty)
                    ?.let(::Category)
                    ?: Timeline
            value.startsWith("$TYPE_SOURCE$SEPARATOR") ->
                value.substringAfter(SEPARATOR)
                    .takeIf(String::isNotEmpty)
                    ?.let(::Source)
                    ?: Timeline
            else -> Timeline
        }
    }
}
