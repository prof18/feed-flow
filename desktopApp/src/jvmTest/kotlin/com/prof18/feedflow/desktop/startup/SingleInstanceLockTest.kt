package com.prof18.feedflow.desktop.startup

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SingleInstanceLockTest {

    private val directory: File = createShortTempDirectory("fflock")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `a second process cannot take the lock until the first one exits`() {
        val child = startChildHoldingLock()
        try {
            val line = child.inputStream.bufferedReader().readLine()
            assertEquals("LOCKED", line)

            assertEquals(SingleInstanceLock.Result.HeldByOther, SingleInstanceLock().acquire(directory))
        } finally {
            child.outputStream.close()
            child.waitFor(CHILD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            child.destroyForcibly()
        }

        val lock = SingleInstanceLock()
        assertEquals(SingleInstanceLock.Result.Acquired, lock.acquire(directory))
        assertEquals("held", lock.state)
    }

    @Test
    fun `a second launch brings the running instance forward`() {
        val child = startChildHoldingLock()
        try {
            val output = child.inputStream.bufferedReader()
            assertEquals("LOCKED", output.readLine())

            assertTrue(InstanceActivation.requestActivation(directory))
            assertEquals("ACTIVATED", output.readLine())
        } finally {
            child.outputStream.close()
            child.waitFor(CHILD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            child.destroyForcibly()
        }
    }

    @Test
    fun `a folder that cannot hold the lock file is Unavailable, not a failure`() {
        val notADirectory = File(directory, "file").apply { writeText("x") }

        val lock = SingleInstanceLock()

        assertEquals(SingleInstanceLock.Result.Unavailable, lock.acquire(notADirectory))
        assertEquals("unavailable", lock.state)
    }

    private fun startChildHoldingLock(): Process {
        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        // The test classpath exceeds the Windows command line limit, so it goes through an argument file.
        // Backslashes are escapes inside argument files; forward slashes work on every OS.
        val classPath = System.getProperty("java.class.path").replace('\\', '/')
        val argFile = File(directory, "child.args").apply { writeText("-cp \"$classPath\"") }
        return ProcessBuilder(
            javaBin,
            "@${argFile.absolutePath}",
            "com.prof18.feedflow.desktop.startup.SingleInstanceLockChildKt",
            directory.absolutePath,
        ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
    }

    private companion object {
        const val CHILD_TIMEOUT_SECONDS = 10L
    }
}
