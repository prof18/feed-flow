package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedItem
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object FeedItemGenerator {

    val feedItemArb = arbitrary {
        FeedItem(
            id = Uuid.random().toString(),
            url = Arb.domain().map { "https://$it/article/${Uuid.random()}" }.bind(),
            title = Arb.string(10..100).bind(),
            subtitle = Arb.string(50..500).orNull(0.3).bind(),
            content = Arb.string(100..2000).orNull(0.5).bind(),
            imageUrl = Arb.domain().map { "https://$it/image.jpg" }.orNull(0.4).bind(),
            feedSource = FeedSourceGenerator.feedSourceArb.bind(),
            pubDateMillis = Arb.long(
                min = 0,
                max = 2000000000000L
            ).orNull(0.1).bind(),
            isRead = Arb.boolean().bind(),
            dateString = Arb.string(10..20).orNull(0.1).bind(),
            commentsUrl = Arb.domain().map { "https://$it/comments" }.orNull(0.8).bind(),
            isBookmarked = Arb.boolean().bind(),
        )
    }

    fun unreadFeedItemArb() = arbitrary {
        feedItemArb.bind().copy(isRead = false)
    }

    fun bookmarkedFeedItemArb() = arbitrary {
        feedItemArb.bind().copy(isBookmarked = true)
    }

    fun feedItemsForSource(feedSource: com.prof18.feedflow.core.model.FeedSource, count: Int = 10) = arbitrary {
        List(count) { feedItemArb.bind().copy(feedSource = feedSource) }
    }
}
