package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.CloudArticleFlag
import com.prof18.feedflow.database.CloudPendingArticleFlag
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PendingCloudChangesManager(
    private val database: DatabaseHelper,
    private val syncDatabase: SyncedDatabaseHelper,
    private val accounts: AccountsRepository,
    private val settings: SettingsRepository,
) {
    private val initialization = Mutex()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isUploadRequired = combine(accounts.currentAccountState, settings.cloudSyncSessionState) { _, _ ->
        if (accounts.isSyncEnabled()) currentSession() else null
    }.flatMapLatest { session ->
        if (session == null) {
            flowOf(false)
        } else {
            combine(
                database.observeCloudPendingArticleFlags(session),
                settings.isSyncUploadRequired,
            ) { pending, dirty ->
                pending || dirty
            }
        }
    }

    private fun currentSession(): String = "${accounts.getCurrentSyncAccount().name}-${settings.cloudSyncSession()}"

    suspend fun sessionForEdit(): String? = initialization.withLock {
        if (!accounts.isSyncEnabled()) return@withLock null
        val session = currentSession()
        database.ensureCloudSyncState(session)
        checkAccountSession(session)
        session
    }

    fun checkAccountSession(session: String) {
        check(accounts.isSyncEnabled() && currentSession() == session) { "Cloud sync account changed" }
    }

    suspend fun hasPendingChanges(): Boolean {
        val session = sessionForEdit() ?: return false
        return database.getCloudPendingArticleFlags(session).isNotEmpty()
    }

    suspend fun capturePendingChanges(): CloudUploadBatch {
        val session = requireNotNull(sessionForEdit())
        val articleFlags = database.getCloudPendingArticleFlags(session)
        return withAccountSession(session) {
            CloudUploadBatch(session, articleFlags, accounts.getCurrentSyncAccount())
        }
    }

    suspend fun applyChangesToSyncDatabase(batch: CloudUploadBatch) {
        checkAccountSession(batch.session)
        syncDatabase.applyPendingArticleFlags(
            readFields = batch.articleFlags.filter { it.field == CloudArticleFlag.READ }
                .associate { it.itemId to it.value },
            bookmarkFields = batch.articleFlags.filter { it.field == CloudArticleFlag.BOOKMARK }
                .associate { it.itemId to it.value },
        )
    }

    suspend fun markChangesAsUploaded(batch: CloudUploadBatch) {
        checkAccountSession(batch.session)
        database.acknowledgeCloudPendingArticleFlags(batch.session, batch.articleFlags)
        if (database.getCloudPendingArticleFlags(batch.session).isNotEmpty()) settings.setIsSyncUploadRequired(true)
    }

    fun <T> withAccountSession(session: String?, block: () -> T): T = settings.withCloudSession {
        if (session != null) checkAccountSession(session)
        block()
    }
}

internal data class CloudUploadBatch(
    val session: String,
    val articleFlags: List<CloudPendingArticleFlag>,
    val account: SyncAccounts,
)
