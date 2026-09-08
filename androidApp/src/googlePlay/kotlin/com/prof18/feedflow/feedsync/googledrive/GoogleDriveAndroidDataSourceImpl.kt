package com.prof18.feedflow.feedsync.googledrive

import android.content.Context
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.api.services.drive.model.File as GoogleDriveFile

class GoogleDriveAndroidDataSourceImpl(
    private val context: Context,
    private val googleDriveSettings: GoogleDriveSettings,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val httpTransport: HttpTransport = NetHttpTransport(),
    private val accessTokenProvider: (suspend () -> String?)? = null,
) : GoogleDriveDataSourceAndroid {
    override suspend fun isAuthorized(): Boolean = runCatching {
        getAccessToken() != null
    }.getOrDefault(false)

    override fun revokeAccess() {
        googleDriveSettings.clearAll()
    }

    override suspend fun validateAuthorization(): AuthorizationValidationResult {
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
            .build()

        return try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(authorizationRequest)
                .await()

            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                if (pendingIntent != null) {
                    AuthorizationValidationResult.NeedsReAuth(pendingIntent)
                } else {
                    AuthorizationValidationResult.Failed
                }
            } else {
                AuthorizationValidationResult.Valid
            }
        } catch (e: Throwable) {
            logger.d(e) { "Failed to validate authorization" }
            AuthorizationValidationResult.Failed
        }
    }

    /**
     * Can throw also [GoogleDriveNeedsReAuthException]
     */
    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult =
        withDriveClient { client ->
            val cachedFileId = googleDriveSettings.getBackupFileId()
            var fileId = cachedFileId ?: requireUniqueDownloadFileId(
                discoverFileIds(client, downloadParam.fileName),
                downloadParam.fileName,
            ) ?: throw CloudBackupNotFoundException()
            val inputStream = try {
                client.files().get(fileId).executeMediaAsInputStream()
            } catch (e: GoogleJsonResponseException) {
                if (e.statusCode != HTTP_NOT_FOUND || cachedFileId == null) throw e
                fileId = requireUniqueDownloadFileId(
                    discoverFileIds(client, downloadParam.fileName),
                    downloadParam.fileName,
                ) ?: throw CloudBackupNotFoundException()
                client.files().get(fileId).executeMediaAsInputStream()
            }
            inputStream.use { input ->
                downloadParam.outputStream.use { output -> input.copyTo(output) }
            }
            googleDriveSettings.setBackupFileId(fileId)
            GoogleDriveDownloadResult()
        }

    /**
     * Can throw also [GoogleDriveNeedsReAuthException]
     */
    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        withDriveClient { client ->
            val cachedFileId = googleDriveSettings.getBackupFileId()
            val mediaContent = FileContent("application/x-sqlite3", uploadParam.file)
            var mustUpdateResolvedFile = cachedFileId == null

            val fileId = if (cachedFileId != null) {
                try {
                    val metadata = GoogleDriveFile().setName(uploadParam.fileName)
                    client.files().update(cachedFileId, metadata, mediaContent).execute()
                    cachedFileId
                } catch (e: Exception) {
                    if (!isNotFound(e)) throw e
                    mustUpdateResolvedFile = true
                    discoverUniqueFileId(client, uploadParam.fileName)
                }
            } else {
                discoverUniqueFileId(client, uploadParam.fileName)
            }
            if (fileId == null) {
                createNewFile(client, uploadParam.fileName, mediaContent)
            } else if (mustUpdateResolvedFile || fileId != cachedFileId) {
                val metadata = GoogleDriveFile().setName(uploadParam.fileName)
                client.files().update(fileId, metadata, mediaContent).execute()
                googleDriveSettings.setBackupFileId(fileId)
            }
            GoogleDriveUploadResult
        }

    private suspend fun getAccessToken(): String? {
        accessTokenProvider?.let { return it() }

        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
            .build()

        return try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(authorizationRequest)
                .await()

            if (result.hasResolution()) {
                // User needs to re-authorize via UI - can't do this in background
                logger.d { "Authorization requires user interaction" }
                throw GoogleDriveNeedsReAuthException("Authorization requires user interaction")
            } else {
                result.accessToken
            }
        } catch (e: GoogleDriveNeedsReAuthException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to get access token" }
            null
        }
    }

    private fun createDriveClient(accessToken: String): Drive {
        val httpRequestInitializer = HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
        }

        return Drive.Builder(
            httpTransport,
            GsonFactory.getDefaultInstance(),
            httpRequestInitializer,
        ).setApplicationName(GOOGLE_DRIVE_CLIENT_APPLICATION_NAME).build()
    }

    private suspend fun <T> withDriveClient(block: (Drive) -> T): T {
        val accessToken = getAccessToken()
        requireNotNull(accessToken) { "Failed to get access token" }
        val client = createDriveClient(accessToken)
        return withContext(dispatcherProvider.io) {
            block(client)
        }
    }

    private fun createNewFile(client: Drive, fileName: String, mediaContent: FileContent) {
        val fileMetadata = GoogleDriveFile()
            .setName(fileName)
            .setParents(listOf("appDataFolder"))

        val newFile = client.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()

        googleDriveSettings.setBackupFileId(newFile.id)
    }

    private fun discoverUniqueFileId(client: Drive, fileName: String): String? {
        return requireUniqueFileId(discoverFileIds(client, fileName), fileName)
    }

    private fun discoverFileIds(client: Drive, fileName: String): List<String> {
        val fileIds = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val request = client.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("nextPageToken,files(id)")
            if (pageToken != null) request.pageToken = pageToken
            val result = request.execute()
            fileIds += result.files.orEmpty().mapNotNull { it.id }
            pageToken = result.nextPageToken
        } while (pageToken != null)
        return fileIds
    }
}

private fun isNotFound(exception: Throwable): Boolean =
    exception is GoogleJsonResponseException && exception.statusCode == HTTP_NOT_FOUND

private fun requireUniqueFileId(
    fileIds: List<String>,
    fileName: String,
): String? = when (fileIds.size) {
    0 -> null
    1 -> fileIds.single()
    else -> throw GoogleDriveUploadException(
        errorMessage = "Multiple Google Drive backup files found for '$fileName'",
    )
}

private fun requireUniqueDownloadFileId(
    fileIds: List<String>,
    fileName: String,
): String? = when (fileIds.size) {
    0 -> null
    1 -> fileIds.single()
    else -> throw GoogleDriveDownloadException(
        errorMessage = "Multiple Google Drive backup files found for '$fileName'",
    )
}

private const val HTTP_NOT_FOUND = 404
