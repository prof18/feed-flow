package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

class FeedRecoveryRepository internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val settingsRepository: SettingsRepository,
    // True only where a WebsiteSessionHttp is bound
    val supported: Boolean,
) {
    // Profiles acquired by verifications that have not called retireOrphan yet; cleanup leaves them alone
    private val profileMutex = Mutex()
    private val heldProfiles = mutableMapOf<String, Int>()

    val accountEpochState: StateFlow<String> get() = settingsRepository.cloudSyncSessionState
    val accountEpoch: String get() = accountEpochState.value

    fun issueTicket(): RecoveryTicket =
        RecoveryTicket(settingsRepository.cloudSyncSessionState.value, settingsRepository)

    suspend fun records(epoch: String): Map<String, FeedRecoveryRecord> {
        if (!supported) return emptyMap()
        return databaseHelper.recoveryRecords(epoch).associateBy { it.sourceId }
    }

    fun recordsFlow(epoch: String): Flow<List<FeedRecoveryRecord>> =
        if (supported) databaseHelper.recoveryRecordsFlow(epoch) else flowOf(emptyList())

    /** Applies [update] only if the stored record still equals `update.previous`, so a stale refresh cannot win. */
    suspend fun apply(update: RecoveryUpdate, ticket: RecoveryTicket) {
        if (!supported) return
        when (update) {
            is RecoveryUpdate.RecordChallenge -> {
                val origin = RecoveryOrigin.of(update.challenge.effectiveUrl) ?: return
                databaseHelper.replaceRecoveryRecordIf(
                    epoch = ticket.accountEpoch,
                    sourceId = update.source.id,
                    expected = update.previous,
                    record = FeedRecoveryRecord(
                        accountEpoch = ticket.accountEpoch,
                        sourceId = update.source.id,
                        subscriptionUrl = update.source.url,
                        effectiveUrl = update.challenge.effectiveUrl,
                        origin = origin.value,
                        mode = RecoveryMode.VerificationRequired,
                        profileId = update.previous?.profileId,
                        statusCode = update.challenge.statusCode,
                        retryAfter = update.challenge.retryAfter,
                    ),
                    commit = ticket::commit,
                )
            }

            is RecoveryUpdate.ClearRecord -> {
                val applied = databaseHelper.replaceRecoveryRecordIf(
                    epoch = ticket.accountEpoch,
                    sourceId = update.source.id,
                    expected = update.previous,
                    record = null,
                    commit = ticket::commit,
                )
                if (applied) update.previous.profileId?.let { retireIfOrphan(it) }
            }
        }
    }

    suspend fun hasProfiles(): Boolean = databaseHelper.hasRecoveryProfiles()

    suspend fun activeProfile(epoch: String, origin: RecoveryOrigin): FeedRecoveryProfile? =
        databaseHelper.activeRecoveryProfile(epoch, origin.value)

    suspend fun profiles(): List<FeedRecoveryProfile> = databaseHelper.recoveryProfiles()

    suspend fun profile(id: String): FeedRecoveryProfile? =
        databaseHelper.recoveryProfiles().firstOrNull { it.id == id }

    suspend fun isCurrent(profile: FeedRecoveryProfile): Boolean {
        val stored = profile(profile.id) ?: return false
        return !stored.retired && stored.accountEpoch == accountEpochState.value
    }

    suspend fun acquireProfile(
        epoch: String,
        origin: RecoveryOrigin,
        userAgent: String,
        engineVersion: String,
    ): FeedRecoveryProfile = profileMutex.withLock {
        val ticket = issueTicket()
        if (ticket.accountEpoch != epoch) throw StaleRecoveryException()
        val active = activeProfile(epoch, origin)
        if (active != null) {
            if (active.userAgent == userAgent && active.engineVersion == engineVersion) return@withLock hold(active)
            databaseHelper.replaceRecoveryProfile(active.id)
        }
        val profile = FeedRecoveryProfile(
            id = RecoveryPolicy.PROFILE_ID_PREFIX + Uuid.random().toString(),
            accountEpoch = epoch,
            origin = origin.value,
            userAgent = userAgent,
            engineVersion = engineVersion,
            readerRetained = false,
            retired = false,
        )
        databaseHelper.insertRecoveryProfile(profile, ticket::commit)
        hold(profile)
    }

    private fun hold(profile: FeedRecoveryProfile): FeedRecoveryProfile {
        heldProfiles[profile.id] = (heldProfiles[profile.id] ?: 0) + 1
        return profile
    }

    /** [acquireProfile] for the origin of [url]; used from Swift, which cannot build a [RecoveryOrigin]. */
    suspend fun acquireProfileForUrl(
        epoch: String,
        url: String,
        userAgent: String,
        engineVersion: String,
    ): FeedRecoveryProfile = acquireProfile(
        epoch = epoch,
        origin = RecoveryOrigin.of(url) ?: throw WebsiteVerificationFailure(VerificationFailure.Unsupported),
        userAgent = userAgent,
        engineVersion = engineVersion,
    )

    /** Retires the profiles of other account epochs and returns every retired profile to delete natively. */
    suspend fun housekeeping(epoch: String): List<FeedRecoveryProfile> {
        databaseHelper.retireRecoveryProfilesOfOtherEpochs(epoch)
        return databaseHelper.recoveryProfiles().filter { it.retired }
    }

    suspend fun retainForReader(profile: FeedRecoveryProfile, ticket: RecoveryTicket) {
        databaseHelper.retainRecoveryProfileForReader(profile.id, ticket::commit)
    }

    /** Releases a profile taken with [acquireProfile] and retires it if nothing else uses it. */
    suspend fun retireOrphan(profileId: String) = profileMutex.withLock {
        val holds = heldProfiles[profileId] ?: 0
        if (holds > 1) heldProfiles[profileId] = holds - 1 else heldProfiles.remove(profileId)
        if (holds <= 1) databaseHelper.retireOrphanRecoveryProfile(profileId)
    }

    private suspend fun retireIfOrphan(profileId: String) = profileMutex.withLock {
        if (profileId !in heldProfiles) databaseHelper.retireOrphanRecoveryProfile(profileId)
    }

    suspend fun clearWebsite(profileId: String) {
        databaseHelper.retireRecoveryProfile(profileId)
    }

    /** Retires an active profile whose platform website data is gone, unless a verification holds it. */
    suspend fun retireMissingProfile(profileId: String) = profileMutex.withLock {
        if (profileId !in heldProfiles) databaseHelper.retireRecoveryProfile(profileId)
    }

    suspend fun nativeDeletionCompleted(profileId: String) {
        databaseHelper.deleteRetiredRecoveryProfile(profileId)
    }
}

sealed interface RecoveryUpdate {
    data class RecordChallenge(
        val source: FeedSource,
        val challenge: FeedChallenge,
        val previous: FeedRecoveryRecord?,
    ) : RecoveryUpdate

    data class ClearRecord(val source: FeedSource, val previous: FeedRecoveryRecord) : RecoveryUpdate
}
