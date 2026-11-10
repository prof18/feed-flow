package com.prof18.feedflow.desktop.e2e.harness

import dev.sebastiano.spectre.agent.ExperimentalSpectreAgentApi
import dev.sebastiano.spectre.agent.launch.LaunchedSession
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Gives each test an isolated, seeded FeedFlow desktop app. The test body launches it with [start]
 * as its first line; the rule tears it down, and on failure writes evidence under EVIDENCE_DIR.
 */
@OptIn(ExperimentalSpectreAgentApi::class)
class DesktopE2eRule : TestRule {

    @Volatile
    internal var session: LaunchedSession? = null

    @Volatile
    private var language: String = "en"

    private var fixtureServer: FixtureServer? = null
    private val extraProcesses = mutableListOf<Process>()
    internal lateinit var home: E2eHome

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            assumeTrue(
                "The desktop E2E suite runs on macOS only",
                System.getProperty("os.name").contains("Mac", ignoreCase = true),
            )
            check(!isReleaseBuild()) {
                "props.properties has is_release=true: a release app ignores the E2E properties " +
                    "and would open the real data"
            }
            acquireRunLock()
            home = E2eHome.create()
            val evidenceDir = Evidence.dir(description.testClass.simpleName, description.methodName)
            File(evidenceDir, "home.txt").writeText(home.dir.absolutePath)
            DesktopE2eLaunch.removePreferencesNode()

            var failure: Throwable? = null
            try {
                Timeout(FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS).apply(base, description).evaluate()
            } catch (e: AssumptionViolatedException) {
                failure = e
            } catch (e: Throwable) {
                failure = e
                runCatching { Evidence.collect(session, evidenceDir) }
            } finally {
                val cleanupErrors = tearDown()
                if (failure == null && cleanupErrors.isNotEmpty()) {
                    val first = cleanupErrors.first()
                    cleanupErrors.drop(1).forEach(first::addSuppressed)
                    failure = first
                } else {
                    cleanupErrors.forEach { failure?.addSuppressed(it) }
                }
            }
            if (failure == null) home.dir.deleteRecursively()
            failure?.let { throw it }
        }
    }

    /** Starts the fixture server if needed; callable before [start] for launch arguments. */
    fun fixtureUrl(path: String): String {
        val server = fixtureServer ?: FixtureServer.start(repoRoot()).also { fixtureServer = it }
        return server.url(path)
    }

    fun start(seed: Seed?, args: List<String> = emptyList(), language: String = "en"): Flow {
        check(session == null) { "start() was already called" }
        this.language = language
        session = DesktopE2eLaunch.launch(home, seed, args, language)
        return Flow(this)
    }

    internal fun restart(seed: Seed?, args: List<String>) {
        session?.let(::closeSession)
        session = null
        session = DesktopE2eLaunch.launch(home, seed, args, language)
    }

    internal fun runSecondInstance(args: List<String>): Int {
        val command = DesktopE2eLaunch.command(home, seed = null, args = args, language = language)
        val process = ProcessBuilder(command)
            .redirectOutput(File(home.dir, "second-instance-stdout.log"))
            .redirectError(File(home.dir, "second-instance-stderr.log"))
            .start()
        synchronized(extraProcesses) { extraProcesses += process }
        if (!process.waitFor(AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            destroyTree(process.toHandle())
            error("The second instance did not exit within $AWAIT_TIMEOUT_MS ms")
        }
        return process.exitValue()
    }

    private fun tearDown(): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        fun step(block: () -> Unit) {
            runCatching(block).onFailure(errors::add)
        }
        val current = session
        val pid = current?.launchedPid
        step { current?.let(::closeSession) }
        step { pid?.let { ProcessHandle.of(it).orElse(null) }?.let(::destroyTree) }
        step { synchronized(extraProcesses) { extraProcesses.forEach { destroyTree(it.toHandle()) } } }
        step { fixtureServer?.close() }
        step { DesktopE2eLaunch.removePreferencesNode() }
        session = null
        fixtureServer = null
        return errors
    }

    private fun closeSession(target: LaunchedSession) {
        val pid = target.launchedPid
        val closer = thread(name = "desktop-e2e-close", isDaemon = true) { runCatching { target.close() } }
        closer.join(SESSION_CLOSE_TIMEOUT_MS)
        ProcessHandle.of(pid).orElse(null)?.let(::destroyTree)
    }

    private fun destroyTree(handle: ProcessHandle) {
        if (!handle.isAlive) return
        handle.descendants().forEach { it.destroyForcibly() }
        handle.destroyForcibly()
        runCatching { handle.onExit().get(PROCESS_EXIT_TIMEOUT_MS, TimeUnit.MILLISECONDS) }
    }

    private fun isReleaseBuild(): Boolean {
        val stream = javaClass.classLoader.getResourceAsStream("props.properties") ?: return false
        val properties = stream.use { Properties().apply { load(it) } }
        return properties.getProperty("is_release")?.trim()?.toBoolean() == true
    }

    private companion object {
        const val SESSION_CLOSE_TIMEOUT_MS = 10_000L
        const val PROCESS_EXIT_TIMEOUT_MS = 5_000L

        @Volatile
        var runLock: FileLock? = null

        @Synchronized
        fun acquireRunLock() {
            if (runLock != null) return
            File(HOME_ROOT).mkdirs()
            val channel = FileChannel.open(
                File(RUN_LOCK_FILE).toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            runLock = channel.tryLock() ?: run {
                channel.close()
                error("another desktop E2E run is active ($RUN_LOCK_FILE is locked)")
            }
        }
    }
}
