package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedRecoveryRepositoryTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

    private val source = FeedSourceGenerator.feedSource(id = "source-1", url = "https://e.test/feed")
    private val otherSource = FeedSourceGenerator.feedSource(id = "source-2", url = "https://e.test/other")
    private val challenge = FeedChallenge(
        requestedUrl = source.url,
        effectiveUrl = "https://e.test/feed?verified",
        statusCode = 403,
        retryAfter = "120",
    )

    private fun repository(supported: Boolean = true) = FeedRecoveryRepository(
        databaseHelper = databaseHelper,
        settingsRepository = settingsRepository,
        supported = supported,
    )

    private val epoch: String get() = settingsRepository.cloudSyncSessionState.value

    @Test
    fun `unsupported repository has no records and writes nothing`() = runTest(testDispatcher) {
        val repository = repository(supported = false)
        insertSources(source)

        repository.apply(RecoveryUpdate.RecordChallenge(source, challenge, previous = null), repository.issueTicket())

        assertTrue(repository.records(epoch).isEmpty())
        assertTrue(databaseHelper.recoveryRecords(epoch).isEmpty())
    }

    @Test
    fun `recorded challenge is stored as verification required`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source)
        val previous = approvedRecord(source, profileId = "feedflow-recovery-old")
        databaseHelper.upsertRecoveryRecord(previous) { it() }

        repository.apply(RecoveryUpdate.RecordChallenge(source, challenge, previous), repository.issueTicket())

        val stored = repository.records(epoch).getValue(source.id)
        assertEquals(RecoveryMode.VerificationRequired, stored.mode)
        assertEquals(challenge.effectiveUrl, stored.effectiveUrl)
        assertEquals("https://e.test:443", stored.origin)
        assertEquals(403, stored.statusCode)
        assertEquals("120", stored.retryAfter)
        assertEquals("feedflow-recovery-old", stored.profileId)
    }

    @Test
    fun `a stale challenge does not overwrite a freshly approved record`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source)
        val stale = approvedRecord(source, "feedflow-recovery-old").copy(mode = RecoveryMode.VerificationRequired)
        val approved = approvedRecord(source, "feedflow-recovery-new")
        databaseHelper.upsertRecoveryRecord(approved) { it() }

        repository.apply(RecoveryUpdate.RecordChallenge(source, challenge, stale), repository.issueTicket())

        assertEquals(approved, repository.records(epoch).getValue(source.id))
    }

    @Test
    fun `a stale clear does not delete a freshly approved record or retire its profile`() =
        runTest(testDispatcher) {
            val repository = repository()
            insertSources(source)
            val profile = storedProfile("feedflow-recovery-new")
            val stale = approvedRecord(source, "feedflow-recovery-old").copy(mode = RecoveryMode.VerificationRequired)
            val approved = approvedRecord(source, profile.id)
            databaseHelper.upsertRecoveryRecord(approved) { it() }

            repository.apply(RecoveryUpdate.ClearRecord(source, stale), repository.issueTicket())

            assertEquals(approved, repository.records(epoch).getValue(source.id))
            assertFalse(requireNotNull(repository.profile(profile.id)).retired)
        }

    @Test
    fun `a challenge without a previous record is dropped when a record appeared meanwhile`() =
        runTest(testDispatcher) {
            val repository = repository()
            insertSources(source)
            val approved = approvedRecord(source, "feedflow-recovery-new")
            databaseHelper.upsertRecoveryRecord(approved) { it() }

            val update = RecoveryUpdate.RecordChallenge(source, challenge, previous = null)
            repository.apply(update, repository.issueTicket())

            assertEquals(approved, repository.records(epoch).getValue(source.id))
        }

    @Test
    fun `challenge with an http effective url is not stored`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source)

        repository.apply(
            RecoveryUpdate.RecordChallenge(source, challenge.copy(effectiveUrl = "http://e.test/feed"), null),
            repository.issueTicket(),
        )

        assertTrue(repository.records(epoch).isEmpty())
    }

    @Test
    fun `clearing a record retires only unreferenced profiles`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source, otherSource)
        val shared = storedProfile("feedflow-recovery-shared")
        val record = approvedRecord(source, shared.id)
        databaseHelper.upsertRecoveryRecord(record) { it() }
        databaseHelper.upsertRecoveryRecord(approvedRecord(otherSource, shared.id)) { it() }

        repository.apply(RecoveryUpdate.ClearRecord(source, record), repository.issueTicket())

        assertNull(repository.records(epoch)[source.id])
        assertFalse(requireNotNull(repository.profile(shared.id)).retired)

        val otherRecord = repository.records(epoch).getValue(otherSource.id)
        repository.apply(RecoveryUpdate.ClearRecord(otherSource, otherRecord), repository.issueTicket())

        assertTrue(requireNotNull(repository.profile(shared.id)).retired)
    }

    @Test
    fun `clearing a record keeps a profile a verification still holds`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source)
        val origin = requireNotNull(RecoveryOrigin.of(source.url))
        val held = repository.acquireProfile(epoch, origin, "ua", "engine")
        val record = approvedRecord(source, held.id)
        databaseHelper.upsertRecoveryRecord(record) { it() }

        repository.apply(RecoveryUpdate.ClearRecord(source, record), repository.issueTicket())

        assertFalse(requireNotNull(repository.profile(held.id)).retired)

        repository.retireOrphan(held.id)

        assertTrue(requireNotNull(repository.profile(held.id)).retired)
    }

    @Test
    fun `a profile missing natively is kept while a verification holds it`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source)
        val origin = requireNotNull(RecoveryOrigin.of(source.url))
        val held = repository.acquireProfile(epoch, origin, "ua", "engine")
        val again = repository.acquireProfile(epoch, origin, "ua", "engine")
        databaseHelper.upsertRecoveryRecord(approvedRecord(source, held.id)) { it() }

        repository.retireMissingProfile(held.id)
        repository.retireOrphan(again.id)
        repository.retireMissingProfile(held.id)

        assertFalse(requireNotNull(repository.profile(held.id)).retired)

        repository.retireOrphan(held.id)
        assertFalse(requireNotNull(repository.profile(held.id)).retired)
        repository.retireMissingProfile(held.id)

        assertTrue(requireNotNull(repository.profile(held.id)).retired)
    }

    @Test
    fun `acquireProfile reuses a matching profile and replaces a mismatching one`() = runTest(testDispatcher) {
        val repository = repository()
        val origin = requireNotNull(RecoveryOrigin.of(source.url))

        val first = repository.acquireProfile(epoch, origin, "ua", "engine")
        val again = repository.acquireProfile(epoch, origin, "ua", "engine")
        val replaced = repository.acquireProfile(epoch, origin, "ua-2", "engine")

        assertEquals(first.id, again.id)
        assertNotEquals(first.id, replaced.id)
        assertTrue(replaced.id.startsWith(RecoveryPolicy.PROFILE_ID_PREFIX))
        assertTrue(requireNotNull(repository.profile(first.id)).retired)
        assertTrue(repository.isCurrent(replaced))
        assertFalse(repository.isCurrent(first))
    }

    @Test
    fun `replacing a mismatching profile downgrades its records instead of deleting them`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source, otherSource)
        val origin = requireNotNull(RecoveryOrigin.of(source.url))
        val old = repository.acquireProfile(epoch, origin, "ua", "engine")
        databaseHelper.upsertRecoveryRecord(approvedRecord(source, old.id)) { it() }
        databaseHelper.upsertRecoveryRecord(approvedRecord(otherSource, old.id)) { it() }

        val replaced = repository.acquireProfile(epoch, origin, "ua-2", "engine")

        assertNotEquals(old.id, replaced.id)
        assertTrue(requireNotNull(repository.profile(old.id)).retired)
        val records = repository.records(epoch)
        assertEquals(setOf(source.id, otherSource.id), records.keys)
        records.values.forEach {
            assertEquals(RecoveryMode.VerificationRequired, it.mode)
            assertNull(it.profileId)
            assertNull(it.statusCode)
            assertNull(it.retryAfter)
        }
    }

    @Test
    fun `housekeeping retires and returns profiles of other epochs`() = runTest(testDispatcher) {
        val repository = repository()
        val origin = requireNotNull(RecoveryOrigin.of(source.url))
        val old = repository.acquireProfile(epoch, origin, "ua", "engine")
        settingsRepository.rotateCloudSyncSession()

        val toDelete = repository.housekeeping(epoch)

        assertEquals(listOf(old.id), toDelete.map { it.id })
        assertTrue(toDelete.single().retired)
    }

    @Test
    fun `clearWebsite retires the profile and drops its records and cache info`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source)
        val origin = requireNotNull(RecoveryOrigin.of(source.url))
        val profile = repository.acquireProfile(epoch, origin, "ua", "engine")
        databaseHelper.upsertRecoveryRecord(approvedRecord(source, profile.id)) { it() }
        databaseHelper.updateFeedSourcesCacheInfo(listOf(cacheInfo(source.id)))

        repository.clearWebsite(profile.id)

        assertTrue(requireNotNull(repository.profile(profile.id)).retired)
        assertTrue(repository.records(epoch).isEmpty())
        assertTrue(databaseHelper.getFeedSourcesCacheInfo().none { it.feedSourceId == source.id })
    }

    @Test
    fun `apply with a revoked ticket throws`() = runTest(testDispatcher) {
        val repository = repository()
        insertSources(source)
        val ticket = repository.issueTicket()
        ticket.revoke()

        assertFailsWith<StaleRecoveryException> {
            repository.apply(RecoveryUpdate.RecordChallenge(source, challenge, null), ticket)
        }
        assertTrue(repository.records(epoch).isEmpty())
    }

    private suspend fun storedProfile(id: String): FeedRecoveryProfile = FeedRecoveryProfile(
        id = id,
        accountEpoch = epoch,
        origin = "https://e.test:443",
        userAgent = "ua",
        engineVersion = "engine",
        readerRetained = false,
        retired = false,
    ).also { databaseHelper.insertRecoveryProfile(it) { block -> block() } }

    private suspend fun insertSources(vararg sources: FeedSource) {
        databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
    }

    private fun approvedRecord(source: FeedSource, profileId: String) = FeedRecoveryRecord(
        accountEpoch = epoch,
        sourceId = source.id,
        subscriptionUrl = source.url,
        effectiveUrl = source.url,
        origin = "https://e.test:443",
        mode = RecoveryMode.SessionApproved,
        profileId = profileId,
        statusCode = 403,
        retryAfter = null,
    )

    private fun cacheInfo(sourceId: String) = FeedSourceCacheInfo(
        feedSourceId = sourceId,
        etag = "etag",
        lastModified = null,
        validatorsTimestamp = null,
        nextFetchTimestamp = null,
        backoffTimestamp = null,
        userAgentTier = null,
    )
}
