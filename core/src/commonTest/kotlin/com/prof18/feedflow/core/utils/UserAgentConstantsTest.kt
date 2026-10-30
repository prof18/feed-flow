package com.prof18.feedflow.core.utils

import com.prof18.feedflow.core.model.FeedFetchTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserAgentConstantsTest {
    @Test
    fun browserHeadersPreserveTransportDecompression() {
        assertFalse(FEEDFLOW_BROWSER_FALLBACK_HEADERS.keys.any { it.equals("Accept-Encoding", ignoreCase = true) })
        assertTrue(
            FEEDFLOW_BROWSER_FALLBACK_HEADERS.keys.containsAll(
                listOf("Sec-Fetch-Dest", "Sec-Fetch-Mode", "Sec-Fetch-Site", "Sec-Fetch-User"),
            ),
        )
        assertEquals(FEEDFLOW_READER_FALLBACK_USER_AGENT, FEEDFLOW_BROWSER_FALLBACK_HEADERS["User-Agent"])
    }

    @Test
    fun fetchTiersKeepTheirAttemptOrder() {
        assertEquals(
            listOf(FeedFetchTier.PRIMARY, FeedFetchTier.LINK_FREE, FeedFetchTier.BROWSER),
            FeedFetchTier.entries,
        )
    }
}
