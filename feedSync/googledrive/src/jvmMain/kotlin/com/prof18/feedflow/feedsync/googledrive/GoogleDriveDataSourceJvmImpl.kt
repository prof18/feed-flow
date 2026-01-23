package com.prof18.feedflow.feedsync.googledrive

import co.touchlab.kermit.Logger
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import com.google.api.services.drive.model.File as GoogleDriveFile

class GoogleDriveDataSourceJvmImpl(
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val googleDriveSettings: GoogleDriveSettings,
    private val appEnvironment: AppEnvironment,
) : GoogleDriveDataSourceJvm {

    private var driveService: Drive? = null

    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val dataPath: File
        get() = File("${AppDataPathBuilder.getAppDataPath(appEnvironment)}/gdata")

    override suspend fun startAuthFlow(): Boolean = withContext(dispatcherProvider.io) {
        try {
            val flow = buildAuthFlow()

            @Suppress("MagicNumber")
            val receiver = LocalServerReceiver.Builder().setPort(8888).build()
            val credential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")

            driveService = Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(GOOGLE_DRIVE_CLIENT_APPLICATION_NAME)
                .build()

            googleDriveSettings.setGoogleDriveLinked(true)
            true
        } catch (e: Exception) {
            logger.e(e) { "Error during Google Drive auth flow" }
            false
        }
    }

    override fun restoreAuth(): Boolean {
        if (!googleDriveSettings.isGoogleDriveLinked()) {
            return false
        }
        return try {
            val flow = buildAuthFlow()
            val credential = flow.loadCredential("user")
            @Suppress("MagicNumber")
            if (credential != null &&
                (
                    credential.refreshToken != null ||
                        credential.expiresInSeconds == null ||
                        credential.expiresInSeconds > 60
                    )
            ) {
                driveService = Drive.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(GOOGLE_DRIVE_CLIENT_APPLICATION_NAME)
                    .build()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.e(e) { "Error restoring Google Drive auth" }
            false
        }
    }

    override suspend fun revokeAccess() = withContext(dispatcherProvider.io) {
        driveService = null
        googleDriveSettings.clearAll()
        if (dataPath.exists()) {
            dataPath.deleteRecursively()
        }
    }

    override fun isClientSet(): Boolean = driveService != null

    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        withDriveClient { client ->
            val cachedFileId = googleDriveSettings.getBackupFileId()
            val mediaContent = FileContent("application/x-sqlite3", uploadParam.file)
            if (cachedFileId != null) {
                try {
                    val metadata = GoogleDriveFile().setName(uploadParam.fileName)
                    client.files().update(cachedFileId, metadata, mediaContent).execute()
                } catch (_: Exception) {
                    logger.d { "Failed to update existing file, creating new one" }
                    createNewFile(client, uploadParam.fileName, mediaContent)
                }
            } else {
                createNewFile(client, uploadParam.fileName, mediaContent)
            }
            GoogleDriveUploadResult
        }

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

    private fun buildAuthFlow(): GoogleAuthorizationCodeFlow {
        val inStream = GoogleDriveDataSourceJvmImpl::class.java.getResourceAsStream("/credentials.json")
        requireNotNull(inStream) { "Resource not found: /credentials.json" }
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inStream))

        return GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            jsonFactory,
            clientSecrets,
            listOf(DriveScopes.DRIVE_APPDATA),
        )
            .setDataStoreFactory(FileDataStoreFactory(dataPath))
            .setAccessType("offline")
            .build()
    }

    private suspend fun <T> withDriveClient(block: (Drive) -> T): T {
        if (driveService == null) {
            restoreAuth()
        }
        val client = requireNotNull(driveService) { "Drive client not initialized" }
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
