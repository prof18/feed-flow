package com.prof18.feedflow.feedsync.lan

import co.touchlab.kermit.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class LanSyncServer(
    private val settings: LanSyncSettings,
    private val getDatabaseFileBytes: suspend () -> ByteArray?,
    private val logger: Logger,
) {
    private var server: ApplicationEngine? = null
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        if (server != null) {
            logger.d { "LAN sync server already running" }
            return
        }

        val port = settings.getServerPort()
        val deviceId = settings.getDeviceId() ?: return
        val deviceName = settings.getDeviceName() ?: "FeedFlow Device"

        serverJob = serverScope.launch {
            try {
                server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        })
                    }

                    routing {
                        get("/") {
                            call.respond(
                                LanSyncResponse(
                                    success = true,
                                    message = "FeedFlow LAN Sync Server",
                                ),
                            )
                        }

                        get("/metadata") {
                            val metadata = LanSyncMetadata(
                                deviceId = deviceId,
                                deviceName = deviceName,
                                timestamp = Clock.System.now().toEpochMilliseconds(),
                            )
                            call.respond(metadata)
                        }

                        get("/sync-database") {
                            try {
                                val dbBytes = getDatabaseFileBytes()
                                if (dbBytes != null) {
                                    call.respondBytes(
                                        bytes = dbBytes,
                                        contentType = ContentType.Application.OctetStream,
                                    )
                                } else {
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        LanSyncResponse(
                                            success = false,
                                            message = "Failed to read database file",
                                        ),
                                    )
                                }
                            } catch (e: Exception) {
                                logger.e(e) { "Error serving sync database" }
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    LanSyncResponse(
                                        success = false,
                                        message = "Error: ${e.message}",
                                    ),
                                )
                            }
                        }
                    }
                }.start(wait = false)

                logger.d { "LAN sync server started on port $port" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to start LAN sync server" }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        server?.stop(1000, 2000)
        server = null
        logger.d { "LAN sync server stopped" }
    }

    fun isRunning(): Boolean = server != null
}
