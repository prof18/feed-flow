package com.prof18.feedflow.feedsync.googledrive

import co.touchlab.kermit.Logger
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.prof18.feedflow.core.model.CloudBackupNotFoundException
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
    private val httpTransport: HttpTransport = NetHttpTransport(),
    driveService: Drive? = null,
) : GoogleDriveDataSourceJvm {

    private var driveService: Drive? = driveService

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val dataPath: File
        get() = File("${AppDataPathBuilder.getAppDataPath(appEnvironment)}/gdata")

    override suspend fun startAuthFlow(): Boolean = withContext(dispatcherProvider.io) {
        try {
            val flow = buildAuthFlow()
            val receiver = buildLocalServerReceiver()
            val credential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")

            driveService = Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(GOOGLE_DRIVE_CLIENT_APPLICATION_NAME)
                .build()

            googleDriveSettings.setGoogleDriveLinked(true)
            true
        } catch (e: Throwable) {
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
            var mustUpdateResolvedFile = cachedFileId == null
            val fileId = if (cachedFileId != null) {
                try {
                    val metadata = GoogleDriveFile().setName(uploadParam.fileName)
                    client.files().update(cachedFileId, metadata, mediaContent).execute()
                    cachedFileId
                } catch (e: Exception) {
                    if (!isNotFound(e)) {
                        throw e
                    }
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
            try {
                block(client)
            } catch (e: TokenResponseException) {
                if (e.details?.error == "invalid_grant") {
                    logger.d(e) { "Google Drive token expired or revoked, needs re-auth" }
                    revokeAccess()
                    throw GoogleDriveNeedsReAuthException(
                        errorMessage = "Google Drive token expired or revoked",
                    )
                }
                throw e
            }
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

internal fun requireGoogleDriveBackupFileId(
    fileId: String?,
    fileName: String,
): String = fileId ?: throw GoogleDriveDownloadException(
    errorMessage = "No Google Drive backup file found for '$fileName'",
)

// Bind to the loopback IP literal rather than the "localhost" hostname: Google's own OAuth docs
// note that "localhost" can be blocked or misrouted by client firewalls/DNS, while 127.0.0.1 is not.
internal fun buildLocalServerReceiver(): LocalServerReceiver = LocalServerReceiver.Builder()
    .setHost("127.0.0.1")
    .build()

private const val HTTP_NOT_FOUND = 404
