package com.prof18.feedflow.shared.domain.feed

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel

internal interface RssParserWrapper {
    suspend fun getRssChannel(url: String): RssChannel
}

internal class RssParserWrapperImpl(
    private val parser: RssParser,
) : RssParserWrapper {
    override suspend fun getRssChannel(url: String): RssChannel =
        parser.getRssChannel(url)
}
