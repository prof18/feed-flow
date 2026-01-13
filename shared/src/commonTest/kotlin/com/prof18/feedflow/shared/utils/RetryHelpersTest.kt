package com.prof18.feedflow.shared.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryHelpersTest {

    @Test
    fun `should retry specified number of times`() {
        var attempts = 0

        assertFailsWith<RuntimeException> {
            executeWithRetry(maxRetries = 3) {
                attempts++
                throw RuntimeException("Network error")
            }
        }

        assertEquals(3, attempts)
    }

    @Test
    fun `should succeed on first attempt if no error`() {
        var attempts = 0

        val result = executeWithRetry(maxRetries = 3) {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `should succeed after some retries`() {
        var attempts = 0

        val result = executeWithRetry(maxRetries = 5) {
            attempts++
            if (attempts < 3) {
                throw RuntimeException("Temporary error")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `should work with maxRetries of 1`() {
        var attempts = 0

        assertFailsWith<RuntimeException> {
            executeWithRetry(maxRetries = 1) {
                attempts++
                throw RuntimeException("Error")
            }
        }

        assertEquals(1, attempts)
    }
}
