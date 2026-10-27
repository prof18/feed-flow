package com.prof18.feedflow.database

import co.touchlab.kermit.Logger
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/**
 * The database file and its WAL/SHM sidecars, moved aside together when the database is corrupt.
 *
 * A marker holding the recovery id is written before the first move, so a crash between moves is
 * finished on the next launch instead of leaving an old WAL next to a new database.
 */
internal class DatabaseFileSet(
    databasePath: File,
    private val moveFile: (from: Path, to: Path) -> Unit,
    private val deleteFile: (Path) -> Unit,
    private val logger: Logger,
) {
    private val directory: File = databasePath.absoluteFile.parentFile
    private val baseName: String = databasePath.name
    private val marker = File(directory, "$baseName$MARKER_SUFFIX")

    /** Finishes a quarantine a previous launch left halfway. Returns the recovered file name, if any. */
    fun finishInterruptedQuarantine(): String? {
        if (!marker.exists()) return null
        return storageGuard {
            val recoveryId = marker.readText().trim()
            if (!RECOVERY_ID_FORMAT.matches(recoveryId)) {
                throw IOException("Unreadable quarantine marker")
            }
            logger.w { "Finishing an interrupted database quarantine" }
            moveAll(recoveryId)
            deleteFile(marker.toPath())
            recoveryName(baseName, recoveryId)
        }
    }

    /** Moves the database files aside. Returns the recovered main-file name. */
    fun quarantine(now: Instant): String = storageGuard {
        val recoveryId = freeRecoveryId(TIMESTAMP_FORMAT.format(now.toJavaInstant()))
        writeMarker(recoveryId)
        moveAll(recoveryId)
        deleteFile(marker.toPath())
        recoveryName(baseName, recoveryId)
    }

    private fun freeRecoveryId(timestamp: String): String =
        generateSequence(0) { it + 1 }
            .map { attempt -> if (attempt == 0) timestamp else "$timestamp-$attempt" }
            .first { id -> fileNames().none { File(directory, recoveryName(it, id)).exists() } }

    private fun writeMarker(recoveryId: String) {
        FileChannel.open(
            marker.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
        ).use { channel ->
            channel.write(ByteBuffer.wrap(recoveryId.toByteArray(Charsets.UTF_8)))
            channel.force(true)
        }
    }

    // Main file first: once it is gone, a relaunch can never pair the old WAL with a new database,
    // because the marker makes the next launch move the sidecars before SQLite opens anything.
    private fun moveAll(recoveryId: String) {
        fileNames()
            .map { File(directory, it) }
            .filter { it.exists() }
            .forEach { source ->
                val destination = File(directory, recoveryName(source.name, recoveryId))
                moveFile(source.toPath(), destination.toPath())
                logger.d { "Moved ${source.name} to ${destination.name}" }
            }
    }

    private fun fileNames(): List<String> = listOf(baseName, "$baseName-wal", "$baseName-shm")

    private fun <T> storageGuard(block: () -> T): T =
        try {
            block()
        } catch (e: IOException) {
            logger.e(e) { "Could not move the database files aside" }
            throw DesktopDatabaseStartupException(
                DesktopDatabaseFailure(DesktopDatabaseFailureKind.StorageUnavailable, sqliteResultCode = null),
                e,
            )
        }

    private companion object {
        const val MARKER_SUFFIX = ".quarantine-pending"
        val RECOVERY_ID_FORMAT = Regex("""\d{8}T\d{6}Z(-\d+)?""")
        val TIMESTAMP_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

        fun recoveryName(fileName: String, recoveryId: String) = "$fileName.recovery-$recoveryId"
    }
}
