package com.prof18.feedflow.shared.presentation

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.DropboxConnectionUiState
import com.prof18.feedflow.core.model.DropboxSynMessages
import com.prof18.feedflow.core.model.DropboxSyncUIState
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxException
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.getDxCredentialsAsString
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DropboxSyncViewModel internal constructor(
    private val logger: Logger,
    private val dropboxSettings: DropboxSettings,
    private val dropboxDataSource: DropboxDataSource,
    private val feedSyncRepository: FeedSyncRepository,
    private val dateFormatter: DateFormatter,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private val dropboxSyncUiMutableState = MutableStateFlow<DropboxConnectionUiState>(
        DropboxConnectionUiState.Unlinked,
    )

    @NativeCoroutinesState
    val dropboxConnectionUiState: StateFlow<DropboxConnectionUiState> = dropboxSyncUiMutableState.asStateFlow()

    private val dropboxSyncMessageMutableState = MutableSharedFlow<DropboxSynMessages>()

    @NativeCoroutines
    val dropboxSyncMessageState: SharedFlow<DropboxSynMessages> = dropboxSyncMessageMutableState.asSharedFlow()

    init {
        restoreDropboxAuth()
    }

    fun saveDropboxAuth() {
        scope.launch {
            try {
                val stringCredentials = getDxCredentialsAsString()
                dropboxDataSource.saveAuth(DropboxStringCredentials(stringCredentials))
                dropboxSettings.setDropboxData(stringCredentials)
                dropboxSyncUiMutableState.update {
                    DropboxConnectionUiState.Linked(
                        syncState = getSyncState(),
                    )
                }
                emitSyncLoading()
                feedSyncRepository.firstSync()
                feedRetrieverRepository.fetchFeeds()
                emitLastSyncUpdate()
            } catch (e: Throwable) {
                logger.e(e) { "Error while trying to auth with Dropbox after getting the code" }
                dropboxSyncMessageMutableState.emit(DropboxSynMessages.Error)
                dropboxSyncUiMutableState.update { DropboxConnectionUiState.Unlinked }
            }
        }
    }

    fun triggerBackup() {
        scope.launch {
            emitSyncLoading()
            feedSyncRepository.performBackup(forceBackup = true)
            emitLastSyncUpdate()
        }
    }

    fun unlink() {
        scope.launch {
            try {
                dropboxSyncUiMutableState.update { DropboxConnectionUiState.Loading }
                dropboxDataSource.revokeAccess()
                dropboxSettings.clearDropboxData()
                feedSyncRepository.deleteAll()
                dropboxSyncUiMutableState.update { DropboxConnectionUiState.Unlinked }
            } catch (_: DropboxException) {
                dropboxSyncMessageMutableState.emit(DropboxSynMessages.Error)
            }
        }
    }

    private fun restoreDropboxAuth() {
        dropboxSyncUiMutableState.update { DropboxConnectionUiState.Loading }
        val stringCredentials = dropboxSettings.getDropboxData()
        if (stringCredentials != null) {
            dropboxDataSource.restoreAuth(DropboxStringCredentials(stringCredentials))
            dropboxSyncUiMutableState.update {
                DropboxConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
        } else {
            dropboxSyncUiMutableState.update { DropboxConnectionUiState.Unlinked }
        }
    }

    private fun getSyncState(): DropboxSyncUIState {
        return when {
            dropboxSettings.getLastDownloadTimestamp() != null || dropboxSettings.getLastUploadTimestamp() != null -> {
                DropboxSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> DropboxSyncUIState.None
        }
    }

    private fun getLastUploadDate(): String? =
        dropboxSettings.getLastUploadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun getLastDownloadDate(): String? =
        dropboxSettings.getLastDownloadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun emitSyncLoading() {
        dropboxSyncUiMutableState.update { oldState ->
            if (oldState is DropboxConnectionUiState.Linked) {
                DropboxConnectionUiState.Linked(
                    syncState = DropboxSyncUIState.Loading,
                )
            } else {
                oldState
            }
        }
    }

    private fun emitLastSyncUpdate() {
        dropboxSyncUiMutableState.update { oldState ->
            if (oldState is DropboxConnectionUiState.Linked) {
                DropboxConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            } else {
                oldState
            }
        }
    }
}
