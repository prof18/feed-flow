package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveCredentials
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSource
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveException
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveStringCredentials
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Properties

class GoogleDriveSyncViewModel internal constructor(
    private val logger: Logger,
    private val googleDriveSettings: GoogleDriveSettings,
    private val googleDriveDataSource: GoogleDriveDataSource,
    private val feedSyncRepository: FeedSyncRepository,
    private val dateFormatter: DateFormatter,
    private val accountsRepository: AccountsRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
) : ViewModel() {

    private var clientId: String? = null
    private var clientSecret: String? = null

    private val googleDriveSyncUiMutableState =
        MutableStateFlow<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
    val googleDriveConnectionUiState: StateFlow<AccountConnectionUiState> = googleDriveSyncUiMutableState.asStateFlow()

    private val googleDriveSyncMessageMutableState = MutableSharedFlow<GoogleDriveSynMessages>()
    val googleDriveSyncMessageState: SharedFlow<GoogleDriveSynMessages> = googleDriveSyncMessageMutableState.asSharedFlow()

    init {
        restoreGoogleDriveAuth()
    }

    fun startGoogleDriveAuthFlow() {
        viewModelScope.launch {
            val properties = Properties()
            val propsFile = GoogleDriveSyncViewModel::class.java.classLoader?.getResourceAsStream("props.properties")
                ?: InputStream.nullInputStream()
            properties.load(propsFile)

            val clientId = properties["google_drive_client_id"]?.toString()
            val clientSecret = properties["google_drive_client_secret"]?.toString()
            this@GoogleDriveSyncViewModel.clientId = clientId
            this@GoogleDriveSyncViewModel.clientSecret = clientSecret

            if (clientId != null && clientSecret != null) {
                try {
                    val redirectUri = "urn:ietf:wg:oauth:2.0:oob"
                    val scope = "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.appdata"
                    val authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=$clientId&" +
                        "redirect_uri=$redirectUri&" +
                        "response_type=code&" +
                        "scope=$scope&" +
                        "access_type=offline&" +
                        "prompt=consent"

                    googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.ProceedToAuth(authorizeUrl))
                } catch (e: Throwable) {
                    logger.e(e) { "Error while trying to auth with Google Drive before getting the code" }
                    googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
                    googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
                }
            } else {
                logger.e { "Google Drive client ID or secret not found in properties" }
                googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
            }
        }
    }

    fun handleGoogleDriveAuthResponse(authorizationCode: String) {
        viewModelScope.launch {
            try {
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }

                val currentClientId = clientId
                val currentClientSecret = clientSecret

                if (currentClientId == null || currentClientSecret == null) {
                    logger.e { "Client ID or secret not available" }
                    googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
                    googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
                    return@launch
                }

                // Exchange authorization code for access token + refresh token
                val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    currentClientId,
                    currentClientSecret,
                    authorizationCode,
                    "urn:ietf:wg:oauth:2.0:oob",
                ).execute()

                val credentials = GoogleDriveCredentials(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAtMillis = System.currentTimeMillis() + (tokenResponse.expiresInSeconds * 1000),
                )

                googleDriveSettings.setGoogleDriveCredentials(credentials)
                googleDriveDataSource.saveAuth(GoogleDriveStringCredentials(credentials.accessToken))
                accountsRepository.setGoogleDriveAccount()
                googleDriveSyncUiMutableState.update {
                    AccountConnectionUiState.Linked(
                        syncState = getSyncState(),
                    )
                }
                emitSyncLoading()
                feedFetcherRepository.fetchFeeds()
                emitLastSyncUpdate()
            } catch (e: Throwable) {
                logger.e(e) { "Error while trying to auth with Google Drive after getting the code" }
                googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            }
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            emitSyncLoading()
            feedSyncRepository.performBackup(forceBackup = true)
            emitLastSyncUpdate()
        }
    }

    private fun restoreGoogleDriveAuth() {
        googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }
        val credentials = googleDriveSettings.getGoogleDriveCredentials()
        if (credentials != null) {
            googleDriveDataSource.restoreAuth(GoogleDriveStringCredentials(credentials.accessToken))
            googleDriveSyncUiMutableState.update {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
        } else {
            googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }
                googleDriveDataSource.revokeAccess()
                googleDriveSettings.clearGoogleDriveData()
                feedSyncRepository.deleteAll()
                accountsRepository.clearAccount()
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            } catch (_: GoogleDriveException) {
                googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
            }
        }
    }

    private fun getLastUploadDate(): String? =
        googleDriveSettings.getLastUploadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun getLastDownloadDate(): String? =
        googleDriveSettings.getLastDownloadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun emitSyncLoading() {
        googleDriveSyncUiMutableState.update { oldState ->
            if (oldState is AccountConnectionUiState.Linked) {
                AccountConnectionUiState.Linked(
                    syncState = AccountSyncUIState.Loading,
                )
            } else {
                oldState
            }
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            googleDriveSettings.getLastDownloadTimestamp() != null || googleDriveSettings.getLastUploadTimestamp() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> AccountSyncUIState.None
        }
    }

    private fun emitLastSyncUpdate() {
        googleDriveSyncUiMutableState.update { oldState ->
            if (oldState is AccountConnectionUiState.Linked) {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            } else {
                oldState
            }
        }
    }
}
