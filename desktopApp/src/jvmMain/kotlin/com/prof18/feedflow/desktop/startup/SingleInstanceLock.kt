package com.prof18.feedflow.desktop.startup

import co.touchlab.kermit.Logger
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption

/**
 * Keeps a second FeedFlow process from opening the same database. The OS releases the lock when the
 * process exits, even after a crash.
 */
internal class SingleInstanceLock(
    private val logger: Logger = Logger.withTag("SingleInstanceLock"),
) {
    enum class Result { Acquired, HeldByOther, Unavailable }

    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    var state: String = "unknown"
        private set

    fun acquire(appDataPath: File): Result {
        val result = try {
            val newChannel = FileChannel.open(
                File(appDataPath, LOCK_FILE_NAME).toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            val newLock = try {
                newChannel.tryLock()
            } catch (_: OverlappingFileLockException) {
                null
            }
            if (newLock == null) {
                newChannel.close()
                Result.HeldByOther
            } else {
                channel = newChannel
                lock = newLock
                Result.Acquired
            }
        } catch (e: IOException) {
            // An odd file system must not lock the user out: continue without the guard.
            logger.w(e) { "Single-instance lock unavailable" }
            Result.Unavailable
        }
        state = when (result) {
            Result.Acquired -> "held"
            Result.Unavailable -> "unavailable"
            Result.HeldByOther -> state
        }
        return result
    }

    private companion object {
        const val LOCK_FILE_NAME = "FeedFlow.lock"
    }
}
