package com.prof18.feedflow.shared.test.generators

import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object RssItemGenerator {
    private val urlArb = Arb.domain().map { "https://$it" }

    private val rfc2822DateArb: Arb<String> = Arb.long(
        min = 1577836800000L, // 2020-01-01T00:00:00Z
        max = 1767225599000L, // 2025-12-31T23:59:59Z
    ).map { epochMillis ->
        Instant.fromEpochMilliseconds(epochMillis).toRfc2822String()
    }

    val rssItemArb = arbitrary {
        RssItem(
            guid = Arb.string(8..24).orNull(0.2).bind(),
            title = Arb.string(5..40).orNull(0.2).bind(),
            author = Arb.string(5..30).orNull(0.6).bind(),
            link = urlArb.orNull(0.2).bind(),
            pubDate = rfc2822DateArb.orNull(0.4).bind(),
            description = Arb.string(10..100).orNull(0.5).bind(),
            content = Arb.string(10..120).orNull(0.6).bind(),
            image = urlArb.orNull(0.6).bind(),
            audio = urlArb.orNull(0.9).bind(),
            video = urlArb.orNull(0.9).bind(),
            sourceName = Arb.string(5..30).orNull(0.7).bind(),
            sourceUrl = urlArb.orNull(0.7).bind(),
            categories = Arb.list(Arb.string(3..20), 0..3).bind(),
            itunesItemData = null,
            commentsUrl = urlArb.orNull(0.7).bind(),
            youtubeItemData = null,
            rawEnclosure = null,
        )
    }
}

object RssChannelGenerator {
    private val urlArb = Arb.domain().map { "https://$it" }

    private val rfc2822DateArb: Arb<String> = Arb.long(
        min = 1577836800000L, // 2020-01-01T00:00:00Z
        max = 1767225599000L, // 2025-12-31T23:59:59Z
    ).map { epochMillis ->
        Instant.fromEpochMilliseconds(epochMillis).toRfc2822String()
    }

    val rssChannelArb = arbitrary {
        RssChannel(
            title = Arb.string(5..50).orNull(0.2).bind(),
            link = urlArb.orNull(0.2).bind(),
            description = Arb.string(10..120).orNull(0.6).bind(),
            image = null,
            lastBuildDate = rfc2822DateArb.orNull(0.7).bind(),
            updatePeriod = Arb.string(5..20).orNull(0.7).bind(),
            items = Arb.list(RssItemGenerator.rssItemArb, 0..5).bind(),
            itunesChannelData = null,
            youtubeChannelData = null,
        )
    }
}

private fun Instant.toRfc2822String(): String {
    val dateTime = this.toLocalDateTime(TimeZone.UTC)
    val dayOfWeekNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val dayOfWeek = dayOfWeekNames[dateTime.dayOfWeek.ordinal]
    val day = dateTime.day.toString().padStart(2, '0')
    val month = monthNames[dateTime.month.number - 1]
    val year = dateTime.year
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    val second = dateTime.second.toString().padStart(2, '0')

    return "$dayOfWeek, $day $month $year $hour:$minute:$second +0000"
}
