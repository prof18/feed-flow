package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.bazqux.BazquxAccountContent
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxAccountContent
import com.prof18.feedflow.desktop.accounts.feedbin.FeedbinAccountContent
import com.prof18.feedflow.desktop.accounts.freshrss.FreshRssAccountContent
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveAccountContent
import com.prof18.feedflow.desktop.accounts.icloud.ICloudAccountContent
import com.prof18.feedflow.desktop.accounts.miniflux.MinifluxAccountContent
import java.util.concurrent.atomic.AtomicInteger

@Composable
internal fun AccountTestContent(fake: FakeDesktopAccount) {
    val state = fake.uiState.value
    val draft = fake.draft
    when (fake.account) {
        SyncAccounts.FRESH_RSS -> FreshRssAccountContent(
            uiState = state,
            credentialsState = draft,
            isLoginLoading = fake.loginLoading.value,
            hasLocalSubscriptions = false,
            onLogin = fake::login,
            onDisconnect = fake::disconnect,
        )
        SyncAccounts.MINIFLUX -> MinifluxAccountContent(
            uiState = state,
            credentialsState = draft,
            isLoginLoading = fake.loginLoading.value,
            hasLocalSubscriptions = false,
            onLogin = fake::login,
            onDisconnect = fake::disconnect,
        )
        SyncAccounts.FEEDBIN -> FeedbinAccountContent(
            uiState = state,
            credentialsState = draft,
            isLoginLoading = fake.loginLoading.value,
            hasLocalSubscriptions = false,
            onLogin = fake::login,
            onDisconnect = fake::disconnect,
        )
        SyncAccounts.BAZQUX -> BazquxAccountContent(
            uiState = state,
            credentialsState = draft,
            isLoginLoading = fake.loginLoading.value,
            hasLocalSubscriptions = false,
            onLogin = fake::login,
            onDisconnect = fake::disconnect,
        )
        SyncAccounts.DROPBOX -> DropboxAccountContent(
            uiState = state,
            credentialsState = draft,
            onStartAuth = fake::startDropbox,
            onConfirmCode = fake::confirmCode,
            onBackup = fake::backup,
            onDisconnect = fake::disconnect,
        )
        SyncAccounts.GOOGLE_DRIVE -> GoogleDriveAccountContent(
            uiState = state,
            credentialsState = draft,
            onConnect = fake::connect,
            onBackup = fake::backup,
            onDisconnect = fake::disconnect,
        )
        SyncAccounts.ICLOUD -> ICloudAccountContent(
            uiState = state,
            credentialsState = draft,
            onConnect = fake::connect,
            onBackup = fake::backup,
            onDisconnect = fake::disconnect,
        )
        SyncAccounts.LOCAL -> error("LOCAL has no provider connection form")
    }
}

internal class FakeDesktopAccount(val account: SyncAccounts) {
    val draft = CredentialsState()
    val uiState = mutableStateOf<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
    val loginLoading = mutableStateOf(false)
    val requests = AtomicInteger()
    val browserStarts = AtomicInteger()
    val disconnects = AtomicInteger()
    val backups = AtomicInteger()
    var submittedCredentials: List<String?>? = null
    var submittedCode: String? = null

    val isServer: Boolean
        get() = account in setOf(
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.FEEDBIN, SyncAccounts.BAZQUX,
        )

    fun populateDraft() {
        draft.serverUrl = TextFieldValue(ENDPOINT)
        draft.username = TextFieldValue(USERNAME)
        draft.password = TextFieldValue(PASSWORD)
        draft.authCode = TextFieldValue(CODE)
    }

    fun login(serverUrl: String?, username: String, password: String) {
        submittedCredentials = listOf(serverUrl, username, password)
        requests.incrementAndGet()
        loginLoading.value = true
    }

    fun connect() {
        requests.incrementAndGet()
        uiState.value = AccountConnectionUiState.Loading
    }

    fun startDropbox() {
        browserStarts.incrementAndGet()
        draft.submissionPending = false
        draft.authCodeReady = true
    }

    fun confirmCode(code: String) {
        submittedCode = code
        connect()
    }

    fun fail() {
        draft.connectionError = ERROR
        draft.connectionErrorRevision++
        draft.submissionPending = false
        draft.authCodePending = false
        loginLoading.value = false
        uiState.value = AccountConnectionUiState.Unlinked
    }

    fun link() {
        loginLoading.value = false
        uiState.value = AccountConnectionUiState.Linked(
            AccountSyncUIState.Synced(lastUploadDate = SYNC_DATE, lastDownloadDate = SYNC_DATE),
        )
    }

    fun disconnect() {
        disconnects.incrementAndGet()
        uiState.value = AccountConnectionUiState.Unlinked
    }

    fun backup() {
        backups.incrementAndGet()
    }

    companion object {
        const val ENDPOINT = "https://sync.example.test"
        const val USERNAME = "synthetic-reader"
        const val PASSWORD = "synthetic-password"
        const val CODE = "synthetic-code"
        const val ERROR = "Synthetic connection rejected"
        const val SYNC_DATE = "Synthetic sync date"
    }
}
