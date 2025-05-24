package com.prof18.feedflow.core.model

enum class FeedOrder(val sqlValue: String) {
    NEWEST_FIRST("DESC"),
    OLDEST_FIRST("ASC"),
}
