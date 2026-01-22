package com.prof18.feedflow.shared.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryHelpersTest {

    @Test
    fun `executeWithRetry retries specified number of times`() {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            executeWithRetry(maxRetries = 3) {
                attempts++
                error("Network error")
            }
        }

        assertEquals(3, attempts)
    }

    @Test
    fun `executeWithRetry succeeds on first attempt when no error`() {
        var attempts = 0

        val result = executeWithRetry(maxRetries = 3) {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `executeWithRetry succeeds after some retries`() {
        var attempts = 0

        val result = executeWithRetry(maxRetries = 5) {
            attempts++
            if (attempts < 3) {
                error("Temporary error")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `executeWithRetry works with maxRetries of 1`() {
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            executeWithRetry(maxRetries = 1) {
                attempts++
                error("Error")
            }
        }

        assertEquals(1, attempts)
    }
}
