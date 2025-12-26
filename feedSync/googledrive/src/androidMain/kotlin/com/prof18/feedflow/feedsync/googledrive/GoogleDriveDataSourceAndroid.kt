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
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveConstants.GOOGLE_DRIVE_CLIENT_APPLICATION_NAME
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.api.services.drive.model.File as GoogleDriveFile

class GoogleDriveDataSourceAndroid(
    private val context: Context,
    private val googleDriveSettings: GoogleDriveSettings,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
) {

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
                logger.e { "Authorization requires user interaction" }
                null
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
            ?: throw IllegalStateException("Failed to obtain access token")
        val client = createDriveClient(accessToken)
        return withContext(dispatcherProvider.io) {
            block(client)
        }
    }

    suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        withDriveClient { client ->
            val cachedFileId = googleDriveSettings.getBackupFileId()
            val mediaContent = FileContent("application/x-sqlite3", uploadParam.file)

            if (cachedFileId != null) {
                try {
                    val metadata = GoogleDriveFile().setName(uploadParam.fileName)
                    client.files().update(cachedFileId, metadata, mediaContent).execute()
                } catch (e: Exception) {
                    // File might have been deleted, create a new one
                    logger.w { "Failed to update existing file, creating new one" }
                    createNewFile(client, uploadParam.fileName, mediaContent)
                }
            } else {
                createNewFile(client, uploadParam.fileName, mediaContent)
            }
            GoogleDriveUploadResult
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

    suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult =
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

            if (fileId == null) {
                throw GoogleDriveDownloadException("No backup file found in Drive")
            }

            val inputStream = client.files().get(fileId).executeMediaAsInputStream()
            downloadParam.outputStream.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            GoogleDriveDownloadResult()
        }

    suspend fun isAuthorized(): Boolean {
        return getAccessToken() != null
    }

    fun revokeAccess() {
        googleDriveSettings.clearAll()
    }
}
