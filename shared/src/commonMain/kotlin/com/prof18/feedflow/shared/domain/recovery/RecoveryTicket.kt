package com.prof18.feedflow.shared.domain.recovery

import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

class RecoveryTicket internal constructor(
    val accountEpoch: String,
    private val settingsRepository: SettingsRepository,
) {
    // Guards only the onRevoke registration; never held while a commit action runs.
    private val lock = Lock()
    private val revoked = AtomicBoolean(false)
    private var onRevoke: (() -> Unit)? = null

    fun revoke() {
        revoked.value = true
        val cancelGuard = lock.withLock { onRevoke.also { onRevoke = null } }
        cancelGuard?.invoke()
    }

    val isValid: Boolean
        get() = !revoked.value && settingsRepository.cloudSyncSessionState.value == accountEpoch

    /** Runs a short, non-suspending write while the owner and account are still current. */
    fun commit(action: () -> Unit) {
        settingsRepository.withCloudSession {
            if (revoked.value || settingsRepository.cloudSyncSession() != accountEpoch) throw StaleRecoveryException()
            action()
        }
    }

    /** Runs [block], cancelling it when the account epoch changes or the ticket is revoked. */
    suspend fun <T> guard(block: suspend () -> T): T = coroutineScope {
        val owner = coroutineContext.job
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            settingsRepository.cloudSyncSessionState.collect {
                if (it != accountEpoch) owner.cancel(CancellationException("Recovery account changed"))
            }
        }
        lock.withLock {
            if (revoked.value) {
                owner.cancel(CancellationException("Recovery owner revoked"))
            } else {
                onRevoke = { owner.cancel(CancellationException("Recovery owner revoked")) }
            }
        }
        try {
            ensureActive()
            block()
        } finally {
            watcher.cancel()
            lock.withLock { onRevoke = null }
        }
    }
}

class StaleRecoveryException : IllegalStateException("Recovery owner changed")
