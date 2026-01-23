package com.prof18.feedflow.shared.test

import com.prof18.feedflow.core.model.DropboxClientStatus
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadResult
import com.prof18.feedflow.feedsync.dropbox.DropboxException
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadResult

class DropboxDataSourceFake : DropboxDataSource {

    var clientSetState: Boolean = false
    var restoreAuthResult: DropboxClientStatus = DropboxClientStatus.LINKED
    var shouldThrowOnRevokeAccess: Boolean = false
    var savedCredentials: DropboxStringCredentials? = null

    var setupCallCount = 0
        private set
    var startAuthorizationCallCount = 0
        private set
    var handleOAuthResponseCallCount = 0
        private set
    var restoreAuthCallCount = 0
        private set
    var saveAuthCallCount = 0
        private set
    var revokeAccessCallCount = 0
        private set
    var performUploadCallCount = 0
        private set
    var performDownloadCallCount = 0
        private set

    override fun setup(apiKey: String) {
        setupCallCount++
    }

    override fun startAuthorization(platformAuthHandler: () -> Unit) {
        startAuthorizationCallCount++
        platformAuthHandler()
    }

    override fun handleOAuthResponse(platformOAuthResponseHandler: () -> Unit) {
        handleOAuthResponseCallCount++
        platformOAuthResponseHandler()
    }

    override fun restoreAuth(stringCredentials: DropboxStringCredentials): DropboxClientStatus {
        restoreAuthCallCount++
        savedCredentials = stringCredentials
        clientSetState = restoreAuthResult == DropboxClientStatus.LINKED
        return restoreAuthResult
    }

    override fun saveAuth(stringCredentials: DropboxStringCredentials) {
        saveAuthCallCount++
        savedCredentials = stringCredentials
    }

    override suspend fun revokeAccess() {
        revokeAccessCallCount++
        if (shouldThrowOnRevokeAccess) {
            throw DropboxException(
                causeException = Exception("Fake cause"),
                errorMessage = "Fake revoke access error",
            )
        }
        clientSetState = false
    }

    override fun isClientSet(): Boolean = clientSetState

    override suspend fun performUpload(uploadParam: DropboxUploadParam): DropboxUploadResult {
        performUploadCallCount++
        return DropboxUploadResult(
            id = "fake-id",
            editDateMillis = 1704067200000L,
            sizeInByte = 1024L,
            contentHash = "fake-hash",
        )
    }

    override suspend fun performDownload(downloadParam: DropboxDownloadParam): DropboxDownloadResult {
        performDownloadCallCount++
        return DropboxDownloadResult(
            id = "fake-id",
            sizeInByte = 1024L,
            contentHash = "fake-hash",
        )
    }

    fun reset() {
        clientSetState = false
        restoreAuthResult = DropboxClientStatus.LINKED
        shouldThrowOnRevokeAccess = false
        savedCredentials = null
        setupCallCount = 0
        startAuthorizationCallCount = 0
        handleOAuthResponseCallCount = 0
        restoreAuthCallCount = 0
        saveAuthCallCount = 0
        revokeAccessCallCount = 0
        performUploadCallCount = 0
        performDownloadCallCount = 0
    }
}
