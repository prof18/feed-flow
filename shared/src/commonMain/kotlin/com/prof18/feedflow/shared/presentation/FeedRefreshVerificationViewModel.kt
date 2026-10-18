package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRequest
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryService
import com.prof18.feedflow.shared.domain.recovery.RecoveryTicket
import com.prof18.feedflow.shared.domain.recovery.VerificationFailure
import com.prof18.feedflow.shared.domain.recovery.WebsiteVerificationFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FeedRefreshVerificationViewModel internal constructor(
    private val feedSources: () -> Flow<List<FeedSource>>,
    private val recoveryRepository: FeedRecoveryRepository,
    private val coordinator: FeedRecoveryCoordinator,
    private val recoveryService: FeedRecoveryService,
    private val databaseHelper: DatabaseHelper,
    private val dateFormatter: DateFormatter,
) : ViewModel() {
    internal constructor(
        feedSourcesRepository: FeedSourcesRepository,
        recoveryRepository: FeedRecoveryRepository,
        coordinator: FeedRecoveryCoordinator,
        recoveryService: FeedRecoveryService,
        databaseHelper: DatabaseHelper,
        dateFormatter: DateFormatter,
    ) : this(
        feedSources = feedSourcesRepository::getFeedSources,
        recoveryRepository = recoveryRepository,
        coordinator = coordinator,
        recoveryService = recoveryService,
        databaseHelper = databaseHelper,
        dateFormatter = dateFormatter,
    )

    val verificationAvailable: Boolean = coordinator.available

    private val busy = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = busy.asStateFlow()

    private val dismissed = MutableStateFlow(emptySet<String>())
    private var ticket: RecoveryTicket? = null

    val pending: StateFlow<List<PendingFeedVerification>> = candidates().transformLatest { candidates ->
        val waits = waitMillis(candidates.map { it.source.id }.toSet())
        val groups = candidates.groupBy { waits[it.source.id] ?: 0L }
        var elapsed = 0L
        var ready = groups[0L].orEmpty()
        emit(ready)
        groups.entries.filter { it.key > 0 }.sortedBy { it.key }.forEach { (waitMillis, group) ->
            delay(waitMillis - elapsed)
            elapsed = waitMillis
            ready = ready + group
            emit(ready)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), emptyList())

    private fun candidates(): Flow<List<PendingFeedVerification>> =
        recoveryRepository.accountEpochState.flatMapLatest { epoch ->
            recoveryRepository.recordsFlow(epoch).flatMapLatest { records ->
                val required = records.filter {
                    it.accountEpoch == epoch && it.mode == RecoveryMode.VerificationRequired
                }
                // Most users have no records: do not watch the feed sources or the cache info for them
                if (required.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(feedSources(), dismissed) { sources, dismissedIds ->
                        required.filterNot { it.sourceId in dismissedIds }
                            .mapNotNull { record ->
                                sources.firstOrNull { it.id == record.sourceId && it.url == record.subscriptionUrl }
                                    ?.let { PendingFeedVerification(it, record) }
                            }
                    }
                }
            }
        }

    fun verify(pending: PendingFeedVerification) {
        if (busy.value) return
        busy.value = true
        val verificationTicket = recoveryRepository.issueTicket()
        ticket = verificationTicket
        viewModelScope.launch {
            try {
                val request = FeedRecoveryRequest.Refresh(pending.source, pending.record)
                recoveryService.recover(request, verificationTicket)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val reason = (e as? WebsiteVerificationFailure)?.reason ?: VerificationFailure.InvalidContent
                coordinator.reportFailure(reason)
            } finally {
                busy.value = false
            }
        }
    }

    /** Hides [pending] until the app returns to the foreground. */
    fun dismiss(pending: PendingFeedVerification) {
        dismissed.update { it + pending.source.id }
    }

    fun onForeground() {
        dismissed.value = emptySet()
    }

    fun cancel() {
        ticket?.revoke()
        ticket = null
    }

    private suspend fun waitMillis(sourceIds: Set<String>): Map<String, Long> {
        if (sourceIds.isEmpty()) return emptyMap()
        val now = dateFormatter.currentTimeMillis()
        return databaseHelper.getFeedSourcesCacheInfo()
            .filter { it.feedSourceId in sourceIds }
            .mapNotNull { info -> info.backoffTimestamp?.let { info.feedSourceId to (it - now).coerceAtLeast(0) } }
            .toMap()
    }
}

data class PendingFeedVerification(val source: FeedSource, val record: FeedRecoveryRecord)
