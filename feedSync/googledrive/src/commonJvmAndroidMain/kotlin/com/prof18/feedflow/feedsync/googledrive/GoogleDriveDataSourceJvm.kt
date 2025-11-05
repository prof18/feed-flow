package com.prof18.feedflow.feedsync.googledrive

import co.touchlab.kermit.Logger
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.prof18.feedflow.core.model.GoogleDriveClientStatus
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class GoogleDriveDataSourceJvm(
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val googleDriveSettings: GoogleDriveSettings,
    private val clientId: String,
    private val clientSecret: String,
) : GoogleDriveDataSource {

    private var driveService: Drive? = null
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    override fun setup(clientId: String) {
        // No op, nothing required on JVM side
    }

    override fun startAuthorization(platformAuthHandler: () -> Unit) = platformAuthHandler()

    override fun handleOAuthResponse(platformOAuthResponseHandler: () -> Unit) {
        // no-op on JVM
    }

    override fun saveAuth(stringCredentials: GoogleDriveStringCredentials) {
        val credential = getCredentialsFromString(stringCredentials.value)
        credential?.let { driveService = createDriveService(it) }
    }

    override fun restoreAuth(stringCredentials: GoogleDriveStringCredentials): GoogleDriveClientStatus {
        if (driveService != null) {
            return GoogleDriveClientStatus.LINKED
        }
        val credential = getCredentialsFromString(stringCredentials.value)
        return if (credential != null) {
            driveService = createDriveService(credential)
            GoogleDriveClientStatus.LINKED
        } else {
            GoogleDriveClientStatus.NOT_LINKED
        }
    }

    private fun createDriveService(credential: GoogleCredential): Drive {
        return Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(GoogleDriveConstants.GOOGLE_DRIVE_CLIENT_IDENTIFIER)
            .build()
    }

    override suspend fun revokeAccess() = withContext(dispatcherProvider.io) {
        try {
            driveService = null
        } catch (e: Exception) {
            val message = "Error during revoking Google Drive access"
            logger.e(e) { message }
            throw GoogleDriveException(e as? Exception ?: Exception(e), e.message ?: message)
        }
    }

    override fun isClientSet(): Boolean = driveService != null

    private suspend fun ensureValidToken() = withContext(dispatcherProvider.io) {
        val credentials = googleDriveSettings.getGoogleDriveCredentials() ?: return@withContext

        if (credentials.expiresAtMillis < System.currentTimeMillis() + 60_000) {
            val refreshToken = credentials.refreshToken
            if (refreshToken != null && clientId.isNotEmpty() && clientSecret.isNotEmpty()) {
                try {
                    logger.d { "Refreshing expired Google Drive access token" }
                    val tokenResponse = GoogleRefreshTokenRequest(
                        httpTransport,
                        jsonFactory,
                        refreshToken,
                        clientId,
                        clientSecret,
                    ).execute()

                    val newCredentials = credentials.copy(
                        accessToken = tokenResponse.accessToken,
                        expiresAtMillis = System.currentTimeMillis() + (tokenResponse.expiresInSeconds * 1000),
                    )

                    googleDriveSettings.setGoogleDriveCredentials(newCredentials)

                    val newGoogleCredential = GoogleCredential().setAccessToken(newCredentials.accessToken)
                    driveService = createDriveService(newGoogleCredential)

                    logger.d { "Successfully refreshed Google Drive access token" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to refresh Google Drive access token" }
                    throw GoogleDriveException(e, "Failed to refresh access token")
                }
            } else {
                // On Android, refresh tokens are managed by Google Play Services.
                // Token refresh should be handled externally before calling API methods.
                // If we reach here with an expired token, the API call may fail.
                logger.w { "Token may be expired and no refresh token available. On Android, ensure getFreshAccessToken() is called before sync operations." }
            }
        }
    }

    private fun getCredentialsFromString(stringCredentials: String): GoogleCredential? {
        return try {
            val accessToken = stringCredentials
            GoogleCredential().setAccessToken(accessToken)
        } catch (e: Exception) {
            logger.e("Unable to create credentials from string", e)
            null
        }
    }

    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult {
        ensureValidToken()
        return suspendCancellableCoroutine { continuation ->
            try {
                val service = requireNotNull(driveService)

                val fileMetadata = File().apply {
                    name = uploadParam.path.substringAfterLast('/')
                    parents = listOf("appDataFolder")
                }

                val mediaContent = FileContent("application/octet-stream", uploadParam.file)

                val existingFiles = service.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name='${fileMetadata.name}'")
                    .execute()
                    .files

                val uploadedFile = if (existingFiles.isNotEmpty()) {
                    service.files().update(existingFiles[0].id, fileMetadata, mediaContent)
                        .setFields("id, modifiedTime, size")
                        .execute()
                } else {
                    service.files().create(fileMetadata, mediaContent)
                        .setFields("id, modifiedTime, size")
                        .execute()
                }

                val uploadResult = GoogleDriveUploadResult(
                    id = uploadedFile.id,
                    editDateMillis = uploadedFile.modifiedTime?.value ?: System.currentTimeMillis(),
                    sizeInByte = uploadedFile.getSize() ?: 0L,
                )
                continuation.resume(uploadResult)
            } catch (e: Exception) {
                logger.e(e) { "Error while uploading data to Google Drive" }
                continuation.resumeWithException(GoogleDriveUploadException(exceptionCause = e))
            }
        }
    }

    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult {
        ensureValidToken()
        return suspendCancellableCoroutine { continuation ->
            try {
                val service = requireNotNull(driveService)

                val fileName = downloadParam.path.substringAfterLast('/')
                val files = service.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name='$fileName'")
                    .setFields("files(id, size)")
                    .execute()
                    .files

                if (files.isEmpty()) {
                    continuation.resumeWithException(
                        GoogleDriveDownloadException("File not found in Google Drive")
                    )
                    return@suspendCancellableCoroutine
                }

                val file = files[0]
                service.files().get(file.id)
                    .executeMediaAndDownloadTo(downloadParam.outputStream)

                val downloadResult = GoogleDriveDownloadResult(
                    id = file.id,
                    sizeInByte = file.getSize() ?: 0L,
                )
                continuation.resume(downloadResult)
            } catch (e: Exception) {
                logger.e(e) { "Error while downloading data from Google Drive" }
                continuation.resumeWithException(GoogleDriveDownloadException(exceptionCause = e))
            }
        }
    }
}
