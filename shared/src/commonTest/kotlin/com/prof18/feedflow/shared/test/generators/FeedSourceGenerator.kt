package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object FeedSourceGenerator {

    val feedSourceArb = arbitrary {
        FeedSource(
            id = Uuid.random().toString(),
            title = Arb.string(5..50).bind(),
            url = Arb.domain().map { "https://$it/feed.xml" }.bind(),
            category = CategoryGenerator.categoryArb.orNull(0.3).bind(),
            lastSyncTimestamp = null,
            logoUrl = Arb.domain().map { "https://$it/logo.png" }.orNull(0.5).bind(),
            websiteUrl = Arb.domain().map { "https://$it" }.orNull(0.5).bind(),
            fetchFailed = Arb.boolean().bind(),
            linkOpeningPreference = Arb.enum<LinkOpeningPreference>().bind(),
            isHiddenFromTimeline = Arb.boolean().bind(),
            isPinned = Arb.boolean().bind(),
            isNotificationEnabled = Arb.boolean().bind(),
        )
    }

    fun feedSourceWithCategory(categoryName: String) = arbitrary {
        val category = CategoryGenerator.categoryArb.bind().copy(title = categoryName)
        feedSourceArb.bind().copy(category = category)
    }
}
