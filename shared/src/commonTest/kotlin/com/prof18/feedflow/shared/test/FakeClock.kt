package com.prof18.feedflow.shared.test

import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(
    private val instant: Instant,
) : Clock {
    override fun now(): Instant = instant

    companion object {
        val DEFAULT = FakeClock(Instant.Companion.parse("2025-06-15T10:30:00Z"))
    }
}