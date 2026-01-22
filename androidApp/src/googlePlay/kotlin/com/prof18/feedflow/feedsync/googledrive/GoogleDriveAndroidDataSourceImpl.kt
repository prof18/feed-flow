package com.prof18.feedflow.feedsync.googledrive

import android.content.Context
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.api.services.drive.model.File as GoogleDriveFile

class GoogleDriveAndroidDataSourceImpl(
    private val context: Context,
    private val googleDriveSettings: GoogleDriveSettings,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
) : GoogleDriveDataSourceAndroid {
    override suspend fun isAuthorized(): Boolean = runCatching {
        getAccessToken() != null
    }.getOrDefault(false)

    override fun revokeAccess() {
        googleDriveSettings.clearAll()
    }

    /**
     * Can throw also [GoogleDriveNeedsReAuthException]
     */
    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult =
        withDriveClient { client ->
            var fileId = googleDriveSettings.getBackupFileId()

            if (fileId == null) {
                val result = client.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '${downloadParam.fileName}' and trashed = false")
                    .setFields("files(id)")
                    .execute()

                fileId = result.files.firstOrNull()?.id

                if (fileId != null) {
                    googleDriveSettings.setBackupFileId(fileId)
                }
            }

            val inputStream = client.files().get(fileId).executeMediaAsInputStream()
            downloadParam.outputStream.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            GoogleDriveDownloadResult()
        }

    /**
     * Can throw also [GoogleDriveNeedsReAuthException]
     */
    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        withDriveClient { client ->
            val cachedFileId = googleDriveSettings.getBackupFileId()
            val mediaContent = FileContent("application/x-sqlite3", uploadParam.file)

            if (cachedFileId != null) {
                try {
                    val metadata = GoogleDriveFile().setName(uploadParam.fileName)
                    client.files().update(cachedFileId, metadata, mediaContent).execute()
                } catch (_: Throwable) {
                    // File might have been deleted, create a new one
                    logger.d { "Failed to update existing file, creating new one" }
                    createNewFile(client, uploadParam.fileName, mediaContent)
                }
            } else {
                createNewFile(client, uploadParam.fileName, mediaContent)
            }
            GoogleDriveUploadResult
        }

    private suspend fun getAccessToken(): String? {
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
            NetHttpTransport(),
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
}
