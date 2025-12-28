package com.prof18.feedflow.shared.domain.feed.suggestions

import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.businessFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.entertainmentFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.gamingFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.lifestyleFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.newsFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.programmingFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.scienceFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.sportsFeeds
import com.prof18.feedflow.shared.domain.feed.suggestions.bycategory.technologyFeeds

val suggestedFeeds: List<SuggestedFeedCategory> = listOf(
    businessFeeds,
    programmingFeeds,
    entertainmentFeeds,
    gamingFeeds,
    lifestyleFeeds,
    newsFeeds,
    scienceFeeds,
    sportsFeeds,
    technologyFeeds,
)
