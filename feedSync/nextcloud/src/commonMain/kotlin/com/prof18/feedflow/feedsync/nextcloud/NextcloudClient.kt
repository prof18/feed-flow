package com.prof18.feedflow.feedsync.nextcloud

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class NextcloudClient(
    private val logger: Logger,
    private val appEnvironment: AppEnvironment,
    private val dispatcherProvider: DispatcherProvider,
    private val nextcloudSettings: NextcloudSettings,
) {
    private var httpClient: HttpClient? = null

    private fun getOrCreateHttpClient(): HttpClient {
        val credentials = getCredentials()
            ?: throw NextcloudException.AuthenticationException("No credentials configured")

        return httpClient ?: createHttpClient(credentials).also {
            httpClient = it
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun createHttpClient(credentials: NextcloudCredentials): HttpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }

            defaultRequest {
                val baseUrl = credentials.serverUrl.let {
                    if (it.endsWith("/")) it else "$it/"
                }
                url(baseUrl)

                val authString = "${credentials.username}:${credentials.password}"
                val encodedAuth = Base64.encode(authString.encodeToByteArray())
                header("Authorization", "Basic $encodedAuth")
            }

            if (appEnvironment.isDebug()) {
                install(Logging) {
                    level = LogLevel.ALL
                    logger = object : io.ktor.client.plugins.logging.Logger {
                        override fun log(message: String) {
                            this@NextcloudClient.logger.d { message }
                        }
                    }
                }
            }
        }

    private fun getCredentials(): NextcloudCredentials? {
        val serverUrl = nextcloudSettings.getServerUrl()
        val username = nextcloudSettings.getUsername()
        val password = nextcloudSettings.getPassword()

        return if (serverUrl != null && username != null && password != null) {
            NextcloudCredentials(serverUrl, username, password)
        } else {
            null
        }
    }

    suspend fun testConnection(): Boolean = withContext(dispatcherProvider.io) {
        try {
            val client = getOrCreateHttpClient()
            val credentials = getCredentials()
                ?: throw NextcloudException.AuthenticationException("No credentials configured")

            val response = client.request("remote.php/dav/files/${credentials.username}/") {
                method = HttpMethod("PROPFIND")
                header("Depth", "0")
            }

            response.status.isSuccess()
        } catch (e: Exception) {
            logger.e(e) { "Nextcloud connection test failed" }
            false
        }
    }

    suspend fun upload(
        remotePath: String,
        fileContent: ByteArray,
    ): NextcloudUploadResult = withContext(dispatcherProvider.io) {
        try {
            val client = getOrCreateHttpClient()
            val credentials = getCredentials()
                ?: throw NextcloudException.AuthenticationException("No credentials configured")

            val webdavPath = "remote.php/dav/files/${credentials.username}$remotePath"

            val response: HttpResponse = client.put(webdavPath) {
                setBody(fileContent)
            }

            if (!response.status.isSuccess()) {
                throw NextcloudException.UploadException(
                    "Upload failed with status: ${response.status}",
                )
            }

            val etag = response.headers["OC-ETag"] ?: response.headers["ETag"]
            val lastModified = System.currentTimeMillis()

            NextcloudUploadResult(
                path = remotePath,
                lastModified = lastModified,
                etag = etag,
                sizeInBytes = fileContent.size.toLong(),
            )
        } catch (e: NextcloudException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Nextcloud upload failed" }
            throw NextcloudException.UploadException("Upload failed", e)
        }
    }

    suspend fun download(remotePath: String): ByteArray = withContext(dispatcherProvider.io) {
        try {
            val client = getOrCreateHttpClient()
            val credentials = getCredentials()
                ?: throw NextcloudException.AuthenticationException("No credentials configured")

            val webdavPath = "remote.php/dav/files/${credentials.username}$remotePath"

            val response: HttpResponse = client.get(webdavPath)

            if (response.status == HttpStatusCode.NotFound) {
                throw NextcloudException.FileNotFoundException("File not found: $remotePath")
            }

            if (!response.status.isSuccess()) {
                throw NextcloudException.DownloadException(
                    "Download failed with status: ${response.status}",
                )
            }

            val channel = response.bodyAsChannel()
            val buffer = ByteArray(channel.availableForRead)
            channel.readAvailable(buffer)
            buffer
        } catch (e: NextcloudException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Nextcloud download failed" }
            throw NextcloudException.DownloadException("Download failed", e)
        }
    }

    suspend fun checkFileExists(remotePath: String): Boolean = withContext(dispatcherProvider.io) {
        try {
            val client = getOrCreateHttpClient()
            val credentials = getCredentials()
                ?: throw NextcloudException.AuthenticationException("No credentials configured")

            val webdavPath = "remote.php/dav/files/${credentials.username}$remotePath"

            val response = client.request(webdavPath) {
                method = HttpMethod("PROPFIND")
                header("Depth", "0")
            }

            response.status.isSuccess()
        } catch (e: Exception) {
            logger.d { "File does not exist: $remotePath" }
            false
        }
    }

    fun closeClient() {
        httpClient?.close()
        httpClient = null
    }
}
