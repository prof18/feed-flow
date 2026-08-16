package com.prof18.feedflow.android.widget.components

import com.prof18.feedflow.android.widget.WidgetImageRequestIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetArticleImageStateTest {

    @Test
    fun `budget key change removes value remembered for old full key`() {
        val oldKey = requestKey(payloadBudgetBytes = 4_096L)
        val currentKey = requestKey(payloadBudgetBytes = 2_048L)
        val oldState = WidgetArticleImageState(
            requestKey = oldKey,
            value = "old bitmap",
        )

        val currentState = oldState.forKey(currentKey)

        assertEquals(currentKey, currentState.requestKey)
        assertNull(currentState.valueFor(currentKey))
    }

    @Test
    fun `completion for stale full key is rejected`() {
        val staleKey = requestKey(payloadBudgetBytes = 4_096L)
        val currentKey = requestKey(payloadBudgetBytes = 2_048L)
        val currentState = WidgetArticleImageState<String>(requestKey = currentKey)

        val result = currentState.accept(
            completedKey = staleKey,
            currentKey = currentKey,
            value = "stale bitmap",
        )

        assertNull(result.valueFor(currentKey))
    }

    @Test
    fun `completion for current full key is accepted for rendering`() {
        val currentKey = requestKey(payloadBudgetBytes = 2_048L)
        val currentState = WidgetArticleImageState<String>(requestKey = currentKey)

        val result = currentState.accept(
            completedKey = currentKey,
            currentKey = currentKey,
            value = "current bitmap",
        )

        assertEquals("current bitmap", result.valueFor(currentKey))
    }

    @Test
    fun `pre S radius key change clears old bitmap rejects stale completion and accepts current`() {
        val requestIdentity = requestKey(payloadBudgetBytes = 4_096L)
        val oldKey = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity,
                renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
                sdkInt = 30,
                cornerRadiusDp = 8,
                displayViewportDp = 50,
            ),
        )
        val currentKey = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity,
                renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
                sdkInt = 30,
                cornerRadiusDp = 32,
                displayViewportDp = 100,
            ),
        )
        val oldState = WidgetArticleImageState(
            requestKey = oldKey,
            value = "old-radius bitmap",
        )

        val currentState = oldState.forKey(currentKey)
        val afterStaleCompletion = currentState.accept(
            completedKey = oldKey,
            currentKey = currentKey,
            value = "stale-radius bitmap",
        )
        val afterCurrentCompletion = afterStaleCompletion.accept(
            completedKey = currentKey,
            currentKey = currentKey,
            value = "current-radius bitmap",
        )

        assertEquals(oldKey.imageUrl, currentKey.imageUrl)
        assertEquals(oldKey.edgePx, currentKey.edgePx)
        assertEquals(oldKey.exactSizeKey, currentKey.exactSizeKey)
        assertEquals(oldKey.payloadBudgetBytes, currentKey.payloadBudgetBytes)
        assertEquals(8, oldKey.softwareCornerRadiusDp)
        assertEquals(32, currentKey.softwareCornerRadiusDp)
        assertNull(currentState.valueFor(currentKey))
        assertNull(afterStaleCompletion.valueFor(currentKey))
        assertEquals("current-radius bitmap", afterCurrentCompletion.valueFor(currentKey))
    }

    private fun requestKey(payloadBudgetBytes: Long) = WidgetImageRequestIdentity(
        imageUrl = "https://example.com/article.png",
        edgePx = 50,
        exactSizeKey = "same-exact-sizes",
        payloadBudgetBytes = payloadBudgetBytes,
    )
}
