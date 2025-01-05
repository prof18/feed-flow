package com.prof18.feedflow.core.model

const val FEED_TITLE_FONT_SIZE = 16
const val FEED_DESC_FONT_SIZE = 14
const val FEED_META_FONT_SIZE = 12

data class FeedFontSizes(
    val feedTitleFontSize: Int = FEED_TITLE_FONT_SIZE,
    val feedDescFontSize: Int = FEED_DESC_FONT_SIZE,
    val feedMetaFontSize: Int = FEED_META_FONT_SIZE,
)

@Suppress("unused") // Used on iOS
fun defaultFeedFontSizes(): FeedFontSizes {
    return FeedFontSizes(
        feedTitleFontSize = FEED_TITLE_FONT_SIZE,
        feedDescFontSize = FEED_DESC_FONT_SIZE,
        feedMetaFontSize = FEED_META_FONT_SIZE,
    )
}

operator fun FeedFontSizes.plus(scaleFactor: Int): FeedFontSizes {
    return FeedFontSizes(
        feedTitleFontSize = feedTitleFontSize + scaleFactor,
        feedDescFontSize = feedDescFontSize + scaleFactor,
        feedMetaFontSize = feedMetaFontSize + scaleFactor,
    )
}
