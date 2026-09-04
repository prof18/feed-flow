package com.prof18.feedflow.shared.domain.feed

import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel

internal interface RssParserWrapper {
    suspend fun getRssChannel(url: String): RssChannel
}

internal class RssParserWrapperImpl(
    private val primaryParser: suspend (String) -> RssChannel,
    private val forbiddenFallbackParser: suspend (String) -> RssChannel,
) : RssParserWrapper {
    override suspend fun getRssChannel(url: String): RssChannel {
        return try {
            primaryParser(url)
        } catch (error: HttpException) {
            if (error.code != HTTP_FORBIDDEN) {
                throw error
            }
            forbiddenFallbackParser(url)
        }
    }

    private companion object {
        const val HTTP_FORBIDDEN = 403
    }
}
