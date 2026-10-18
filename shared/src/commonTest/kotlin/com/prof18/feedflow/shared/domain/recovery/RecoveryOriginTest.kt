package com.prof18.feedflow.shared.domain.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecoveryOriginTest {

    @Test
    fun `https origin is normalised with lowercase host and explicit port`() {
        assertEquals("https://example.com:443", RecoveryOrigin.of("https://Example.com/feed")?.value)
    }

    @Test
    fun `non https or user info or invalid urls have no origin`() {
        assertNull(RecoveryOrigin.of("http://e.test"))
        assertNull(RecoveryOrigin.of("https://u:p@e.test"))
        assertNull(RecoveryOrigin.of("not a url"))
    }
}
