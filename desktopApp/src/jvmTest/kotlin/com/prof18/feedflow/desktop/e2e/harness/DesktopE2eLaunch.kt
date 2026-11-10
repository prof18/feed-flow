package com.prof18.feedflow.desktop.e2e.harness

import com.prof18.feedflow.core.utils.DesktopE2eProperties
import dev.sebastiano.spectre.agent.ExperimentalSpectreAgentApi
import dev.sebastiano.spectre.agent.launch.LaunchAndAttach
import dev.sebastiano.spectre.agent.launch.LaunchSpec
import dev.sebastiano.spectre.agent.launch.LaunchedSession
import java.io.File
import java.util.prefs.Preferences

/** Builds the app command for an E2E home and launches it through Spectre. */
@OptIn(ExperimentalSpectreAgentApi::class)
internal object DesktopE2eLaunch {

    fun command(home: E2eHome, seed: Seed?, args: List<String>, language: String): List<String> = buildList {
        add("${System.getProperty("java.home")}/bin/java")
        add("-cp")
        add(System.getProperty("java.class.path"))
        add("-Duser.language=$language")
        add("-Duser.country=${if (language == "en") "US" else language.uppercase()}")
        add("-D${DesktopE2eProperties.DATA_DIR}=${home.dataDir.absolutePath}")
        add("-D${DesktopE2eProperties.PREFS_NODE}=$PREFS_NODE_VALUE")
        add("-D${DesktopE2eProperties.FILE_DIALOG_RESPONSE}=${home.fileDialogResponse.absolutePath}")
        add("-D${DesktopE2eProperties.OPENED_URI_LOG}=${home.openedUriLog.absolutePath}")
        seed?.let { add("-D${DesktopE2eProperties.SEED}=${it.propertyValue}") }
        seed?.audioUrl?.let { add("-D${DesktopE2eProperties.SEED_AUDIO_URL}=$it") }
        add(MAIN_CLASS)
        addAll(args)
    }

    fun launch(home: E2eHome, seed: Seed?, args: List<String>, language: String): LaunchedSession {
        storeWindowSize()
        val spec = LaunchSpec(
            command = command(home, seed, args, language),
            appJvmNameFilter = MAIN_CLASS,
            captureDirectory = home.spectreDir.toPath(),
        )
        val session = LaunchAndAttach.launch(spec, LaunchAndAttach.DEFAULT_WARNING_SINK)
        try {
            awaitReady(session, home)
        } catch (e: Throwable) {
            runCatching { session.close() }
            throw e
        }
        return session
    }

    fun removePreferencesNode() {
        val root = Preferences.userRoot()
        if (root.nodeExists(PREFS_NODE_VALUE)) {
            root.node(PREFS_NODE_VALUE).removeNode()
            root.flush()
        }
    }

    private fun storeWindowSize() {
        Preferences.userRoot().node(PREFS_NODE_VALUE).apply {
            putInt("DESKTOP_WINDOW_WIDTH_DP", WINDOW_WIDTH_DP)
            putInt("DESKTOP_WINDOW_HEIGHT_DP", WINDOW_HEIGHT_DP)
            flush()
        }
    }

    private fun awaitReady(session: LaunchedSession, home: E2eHome) {
        val process = ProcessHandle.of(session.launchedPid).orElse(null)
        val deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (process != null && !process.isAlive) {
                val stderr = session.stderrPath.toFile().takeIf(File::exists)?.readText().orEmpty()
                if (stderr.contains(SEED_FAILED_MARKER)) error("The E2E seed failed:\n$stderr")
                error("The app exited during startup:\n${stderr.takeLast(STDERR_TAIL)}")
            }
            val ready = runCatching { session.automator.findByTestTag(MAIN_READY_TAG).isNotEmpty() }.getOrDefault(false)
            if (ready) {
                check(File(home.dataDir, "FeedFlow.db").exists()) {
                    "the app ignored ${DesktopE2eProperties.DATA_DIR}: is this a release build?"
                }
                session.automator.waitForIdle()
                return
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        error("The main window did not appear within $STARTUP_TIMEOUT_MS ms")
    }

    private const val STDERR_TAIL = 4_000
}
