package com.prof18.feedflow.core.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Stops the database's live queries while the iOS app is in the background.
 *
 * Every write wakes the queries observing the changed tables, and they re-run after the write's
 * [withSuspensionGuard] has already been released. A re-run that is still reading when iOS suspends
 * the app holds a lock on the app-group database, and the system kills the process (0xdead10cc).
 * While paused, the observers are unsubscribed so no query runs; resuming subscribes them again,
 * and each one emits the current data.
 */
class LiveQueryGate {
    private val isOpen = MutableStateFlow(true)

    fun pause() {
        isOpen.value = false
    }

    fun resume() {
        isOpen.value = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> whileOpen(flow: Flow<T>): Flow<T> = isOpen.flatMapLatest { open -> if (open) flow else emptyFlow() }
}
