package com.prof18.feedflow.feedsync.dropbox

import co.touchlab.kermit.Logger
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.NetworkIOException
import com.dropbox.core.http.HttpRequestor
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DownloadErrorException
import com.dropbox.core.v2.files.UploadErrorException
import com.dropbox.core.v2.files.WriteMode
import com.prof18.feedflow.core.model.CloudBackupNotFoundException
import com.prof18.feedflow.core.model.DropboxClientStatus
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class DropboxDataSourceJvm(
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val httpRequestor: HttpRequestor? = null,
) : DropboxDataSource {

    private var dropboxClient: DbxClientV2? = null

    override fun setup(apiKey: String) {
        // No op, nothing required on Android side
    }

    override fun startAuthorization(platformAuthHandler: () -> Unit) = platformAuthHandler()

    override fun handleOAuthResponse(platformOAuthResponseHandler: () -> Unit) {
        // no-op on Android
    }

    override fun saveAuth(stringCredentials: DropboxStringCredentials) {
        val credentials = getCredentialsFromString(stringCredentials.value)
        credentials?.let { dropboxClient = createClient(credentials) }
    }

    override fun restoreAuth(stringCredentials: DropboxStringCredentials): DropboxClientStatus {
        if (dropboxClient != null) {
            // Avoid setting up again
            return DropboxClientStatus.LINKED
        }
        val credentials = getCredentialsFromString(stringCredentials.value)
        return if (credentials != null) {
            dropboxClient = createClient(credentials)
            DropboxClientStatus.LINKED
        } else {
            DropboxClientStatus.NOT_LINKED
        }
    }

    private fun createClient(credentials: DbxCredential): DbxClientV2 {
        val userLocale: String = Locale.getDefault().toString()
        val requestConfig = DbxRequestConfig
            .newBuilder(DropboxConstants.DROPBOX_CLIENT_IDENTIFIER)
            .withUserLocale(userLocale)
            .apply {
                httpRequestor?.let { withHttpRequestor(it) }
            }
            .build()
        return DbxClientV2(requestConfig, credentials)
    }

    override suspend fun revokeAccess() = withContext(dispatcherProvider.io) {
        val client = requireNotNull(dropboxClient)
        try {
            client.auth().tokenRevoke()
            dropboxClient = null
        } catch (e: DbxException) {
            val message = "Error during revoking dropbox access"
            logger.e(e) { message }
            throw DropboxException(e, e.message ?: message)
        }
    }

    override fun isClientSet(): Boolean =
        dropboxClient != null

    private fun getCredentialsFromString(stringCredentials: String): DbxCredential? {
        return try {
            DbxCredential.Reader.readFully(stringCredentials)
        } catch (e: Exception) {
            logger.d("Unable to create credentials from string", e)
            null
        }
    }

    private fun isTemporaryNetworkError(exception: Exception): Boolean {
        return when (exception) {
            is NetworkIOException -> true
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            else -> {
                val cause = exception.cause
                cause != null && isTemporaryNetworkError(cause as? Exception ?: return false)
            }
        }
    }

    override suspend fun performUpload(uploadParam: DropboxUploadParam): DropboxUploadResult =
        suspendCancellableCoroutine { continuation ->
            try {
                val client = requireNotNull(dropboxClient)
                val writeMode = uploadParam.expectedRevision
                    ?.let(WriteMode::update)
                    ?: WriteMode.ADD
                val metadata = client.files()
                    ?.uploadBuilder(uploadParam.path)
                    ?.withMode(writeMode)
                    ?.withAutorename(false)
                    ?.withStrictConflict(true)
                    ?.uploadAndFinish(FileInputStream(uploadParam.file))

                val id = metadata?.id
                val editTime = metadata?.serverModified
                val size = metadata?.size ?: 0
                val hash = metadata?.contentHash
                logger.d { "Dropbox content hash on upload is: $hash" }

                if (id != null && editTime != null) {
                    val uploadResult = DropboxUploadResult(
                        id = id,
                        editDateMillis = editTime.time,
                        sizeInByte = size,
                        contentHash = hash,
                        revision = metadata.rev,
                    )
                    continuation.resume(uploadResult)
                } else {
                    logger.d { "Metadata from Dropbox are null" }
                    continuation.resumeWithException(DropboxUploadException("Metadata from Dropbox are null"))
                }
            } catch (e: UploadErrorException) {
                if (e.errorValue.isPath && e.errorValue.pathValue.reason.isConflict) {
                    continuation.resumeWithException(DropboxUploadConflictException(e))
                } else {
                    logger.e(e) { "Error while uploading data on Dropbox" }
                    continuation.resumeWithException(DropboxUploadException(exceptionCause = e))
                }
            } catch (e: Exception) {
                if (!isTemporaryNetworkError(e)) {
                    logger.e(e) { "Error while uploading data on Dropbox" }
                }
                continuation.resumeWithException(DropboxUploadException(exceptionCause = e))
            }
        }

    override suspend fun performDownload(downloadParam: DropboxDownloadParam): DropboxDownloadResult =
        suspendCancellableCoroutine { continuation ->
            try {
                val client = requireNotNull(dropboxClient)
                val metadata = client.files()
                    .downloadBuilder(downloadParam.path)
                    .download(downloadParam.outputStream)

                val id = metadata?.id
                val contentHash = metadata?.contentHash
                val sizeInBytes = metadata?.size ?: 0
                val revision = metadata?.rev
                logger.d { "Dropbox content hash on download is: $contentHash" }
                if (id != null) {
                    val downloadResult = DropboxDownloadResult(
                        id = id,
                        sizeInByte = sizeInBytes,
                        contentHash = contentHash,
                        revision = revision,
                    )
                    continuation.resume(downloadResult)
                } else {
                    logger.d { "Metadata from Dropbox are null" }
                    continuation.resumeWithException(DropboxDownloadException("Metadata from Dropbox are null"))
                }
            } catch (e: DownloadErrorException) {
                val error = if (e.errorValue.isPath && e.errorValue.pathValue.isNotFound) {
                    CloudBackupNotFoundException()
                } else {
                    DropboxDownloadException(exceptionCause = e)
                }
                continuation.resumeWithException(error)
            } catch (e: Exception) {
                if (!isTemporaryNetworkError(e)) {
                    logger.e(e) { "Error while downloading data from Dropbox" }
                }
                continuation.resumeWithException(DropboxDownloadException(exceptionCause = e))
            }
        }
}
