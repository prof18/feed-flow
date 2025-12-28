package com.prof18.feedflow.shared.domain.feed.suggestions

import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.businessCategory
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.designCategory
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.newsCategory
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.programmingCategory
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.scienceCategory
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.techCategory

fun getSuggestedFeeds(): List<SuggestedFeedCategory> {
    return listOf(
        techCategory,
        newsCategory,
        programmingCategory,
        scienceCategory,
        designCategory,
        businessCategory,
    )
}
