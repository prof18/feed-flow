package com.prof18.feedflow.android.readermode

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ShownContentSource
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderBackNavigationTest {

    @Test
    fun `web reader exits despite synthetic HTML history`() {
        assertBackDestination(reader(ShownContentSource.WEB), canGoBack = true, expected = "reader")
    }

    @Test
    fun `feed reader exits despite source switch HTML history`() {
        assertBackDestination(reader(ShownContentSource.FEED), canGoBack = true, expected = "reader")
    }

    @Test
    fun `loading reader exits despite history from preceding article`() {
        assertBackDestination(ReaderModeState.Loading, canGoBack = true, expected = "reader")
    }

    @Test
    fun `unavailable URL-less reader exits despite preceding history`() {
        assertBackDestination(unavailable(" "), canGoBack = true, expected = "reader")
    }

    @Test
    fun `fallback browser goes back within navigable history`() {
        assertBackDestination(unavailable("https://example.com/article"), canGoBack = true, expected = "history")
    }

    @Test
    fun `fallback browser exits when there is no history`() {
        assertBackDestination(unavailable("https://example.com/article"), canGoBack = false, expected = "reader")
    }

    private fun assertBackDestination(state: ReaderModeState, canGoBack: Boolean, expected: String) {
        val destinations = mutableListOf<String>()
        navigateReaderBack(
            state = state,
            canGoBack = canGoBack,
            navigateHistory = { destinations.add("history") },
            leaveReader = { destinations.add("reader") },
        )
        assertEquals(listOf(expected), destinations)
    }

    private fun reader(source: ShownContentSource) = ReaderModeState.Success(
        ReaderModeData(
            id = FeedItemId("article"),
            title = "Article",
            content = "Content",
            url = "https://example.com/article",
            baseUrl = "https://example.com",
            fontSize = 16,
            lineHeight = 24,
            isBookmarked = false,
            shownContentSource = source,
            canToggleContentSource = true,
        ),
    )

    private fun unavailable(url: String) = ReaderModeState.ContentNotAvailable(
        url = url,
        id = "article",
        isBookmarked = false,
    )
}
