package com.prof18.feedflow.core.utils

/**
 * Defers app suspension while [block] runs, so locks on the shared app-group container are
 * not held across a suspension. iOS kills a process that is suspended while holding a file
 * or SQLite lock on that container (0xdead10cc); the other platforms have no such constraint
 * and run [block] directly.
 *
 * Asserting background time is not free, so reserve this for work that scales with the amount
 * of synced data rather than applying it to every write.
 */
expect suspend fun <T> withSuspensionGuard(reason: String, block: suspend () -> T): T
