package com.prof18.feedflow.feedsync.lan

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LanSyncClient(
    private val logger: Logger,
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun fetchMetadata(device: LanDevice): LanSyncMetadata? {
        return try {
            val url = "http://${device.ipAddress}:${device.port}/metadata"
            val response = client.get(url)
            if (response.status.isSuccess()) {
                response.body<LanSyncMetadata>()
            } else {
                logger.e { "Failed to fetch metadata from ${device.name}: ${response.status}" }
                null
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching metadata from ${device.name}" }
            null
        }
    }

    suspend fun downloadSyncDatabase(device: LanDevice): ByteArray? {
        return try {
            val url = "http://${device.ipAddress}:${device.port}/sync-database"
            logger.d { "Downloading sync database from ${device.name} at $url" }
            val response = client.get(url)
            if (response.status.isSuccess()) {
                val bytes = response.bodyAsBytes()
                logger.d { "Successfully downloaded ${bytes.size} bytes from ${device.name}" }
                bytes
            } else {
                logger.e { "Failed to download database from ${device.name}: ${response.status}" }
                null
            }
        } catch (e: Exception) {
            logger.e(e) { "Error downloading database from ${device.name}" }
            null
        }
    }

    suspend fun checkConnection(device: LanDevice): Boolean {
        return try {
            val url = "http://${device.ipAddress}:${device.port}/"
            val response = client.get(url)
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.e(e) { "Error checking connection to ${device.name}" }
            false
        }
    }

    fun close() {
        client.close()
    }
}
