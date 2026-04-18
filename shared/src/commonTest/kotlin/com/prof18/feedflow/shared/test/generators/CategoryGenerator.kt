package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedSourceCategory

object CategoryGenerator {
    fun category(
        id: String = "category-id",
        title: String = "Category title",
    ): FeedSourceCategory = FeedSourceCategory(
        id = id,
        title = title,
    )
}
