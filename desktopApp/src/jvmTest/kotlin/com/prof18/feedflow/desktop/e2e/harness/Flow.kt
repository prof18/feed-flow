package com.prof18.feedflow.desktop.e2e.harness

import com.prof18.feedflow.i18n.EnFeedFlowStrings
import dev.sebastiano.spectre.agent.AttachedAutomator
import dev.sebastiano.spectre.agent.ExperimentalSpectreAgentApi
import dev.sebastiano.spectre.agent.SpectreAgentException
import dev.sebastiano.spectre.agent.transport.NodeSnapshotDto
import dev.sebastiano.spectre.agent.transport.WindowSummaryDto
import java.io.File

/**
 * The flow API every desktop E2E test uses. Every await polls until AWAIT_TIMEOUT_MS and fails with
 * the start of the semantics tree; every action waits for the app to be idle afterwards.
 */
@OptIn(ExperimentalSpectreAgentApi::class)
class Flow internal constructor(private val rule: DesktopE2eRule) {
    val strings = EnFeedFlowStrings
    val repoRoot: File = repoRoot()
    val homeDir: File get() = rule.home.dir

    internal val automator: AttachedAutomator
        get() = requireNotNull(rule.session) { "the app is not running" }.automator

    fun awaitText(text: String, exact: Boolean = true, timeoutMs: Long = AWAIT_TIMEOUT_MS): NodeSnapshotDto =
        await("text '$text'", timeoutMs) { automator.findByText(text, exact).firstOrNull { it.isOnScreen } }

    fun awaitTag(tag: String, timeoutMs: Long = AWAIT_TIMEOUT_MS): NodeSnapshotDto =
        await("tag '$tag'", timeoutMs) { automator.findByTestTag(tag).firstOrNull() }

    fun awaitTagPrefix(prefix: String, timeoutMs: Long = AWAIT_TIMEOUT_MS): List<NodeSnapshotDto> =
        await("tag prefix '$prefix'", timeoutMs) { nodesWithTagPrefix(prefix).ifEmpty { null } }

    fun awaitContentDescription(description: String, timeoutMs: Long = AWAIT_TIMEOUT_MS): NodeSnapshotDto =
        await("content description '$description'", timeoutMs) {
            automator.findByContentDescription(description).firstOrNull { it.isOnScreen }
        }

    fun awaitGone(text: String, exact: Boolean = true, timeoutMs: Long = AWAIT_TIMEOUT_MS) {
        await("'$text' to disappear", timeoutMs) {
            Unit.takeIf { automator.findByText(text, exact).none { it.isOnScreen } }
        }
    }

    fun awaitTagGone(tag: String, timeoutMs: Long = AWAIT_TIMEOUT_MS) {
        await("tag '$tag' to disappear", timeoutMs) { Unit.takeIf { automator.findByTestTag(tag).isEmpty() } }
    }

    fun assertAbsent(text: String, exact: Boolean = true) {
        val found = automator.findByText(text, exact).filter { it.isOnScreen }
        check(found.isEmpty()) { "Expected no '$text' but found $found\n${treeHead()}" }
    }

    fun isShown(text: String, exact: Boolean = true): Boolean =
        automator.findByText(text, exact).any { it.isOnScreen }

    fun nodesWithTagPrefix(prefix: String): List<NodeSnapshotDto> =
        automator.allNodes().filter { it.testTag?.startsWith(prefix) == true }

    fun nodes(): List<NodeSnapshotDto> = automator.allNodes()

    fun click(text: String, exact: Boolean = true) = click(awaitText(text, exact))

    fun clickTag(tag: String) = click(awaitTag(tag))

    /**
     * Clicks a node. A window that has just opened can report a node before it is laid out, which
     * Spectre refuses to click; the click is retried briefly with the node's latest snapshot.
     */
    fun click(node: NodeSnapshotDto) {
        var target = node
        // A click on a window that is not focused can be swallowed, so focus the node's window first.
        // Nodes in a popup layer (menus, dropdowns) are skipped: focusing would dismiss the popup.
        if (node.key.split(':').getOrNull(2) == "0") runCatching { automator.focusWindow(node.key) }
        repeat(CLICK_RETRIES) {
            val clicked = runCatching { automator.click(target) }
            if (clicked.isSuccess) {
                idle()
                return
            }
            if (clicked.exceptionOrNull() !is SpectreAgentException) throw requireNotNull(clicked.exceptionOrNull())
            Thread.sleep(CLICK_RETRY_DELAY_MS)
            target = automator.allNodes().firstOrNull { it.key == node.key } ?: target
        }
        automator.click(target)
        idle()
    }

    fun doubleClick(node: NodeSnapshotDto) {
        automator.doubleClick(node.key)
        idle()
    }

    fun typeInto(node: NodeSnapshotDto, text: String) {
        click(node)
        automator.typeText(text)
        idle()
    }

    /** Sends a key to the focused window. FACT shortcuts: menu-bar shortcuts do not arrive over attach. */
    fun key(keyCode: Int, modifiers: Int = 0) {
        automator.pressKey(keyCode, modifiers)
        idle()
    }

    /**
     * CONTEXT_MENU_RULE: FACT context-menu is none and FACT shortcuts is no, so no context menu can be
     * opened over attach. Always false; callers defer the step under T10.8.
     */
    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
    fun openContextMenu(node: NodeSnapshotDto): Boolean = false

    fun scroll(node: NodeSnapshotDto, amount: Int) {
        automator.scrollWheel(node.key, amount)
        idle()
    }

    fun scroll(tag: String, amount: Int) = scroll(awaitTag(tag), amount)

    fun drag(from: NodeSnapshotDto, to: NodeSnapshotDto) {
        automator.swipe(from.key, to.key, DRAG_STEPS, DRAG_DURATION_MS)
        idle()
    }

    fun drag(fromTag: String, toTag: String) = drag(awaitTag(fromTag), awaitTag(toTag))

    fun windows(): List<WindowSummaryDto> = automator.windows()

    fun awaitWindow(title: String, timeoutMs: Long = AWAIT_TIMEOUT_MS): WindowSummaryDto =
        await("window '$title'", timeoutMs) { automator.windows().firstOrNull { it.title == title && it.isShowing } }

    fun awaitWindowGone(title: String, timeoutMs: Long = AWAIT_TIMEOUT_MS) {
        await("window '$title' to close", timeoutMs) {
            Unit.takeIf { automator.windows().none { it.title == title && it.isShowing } }
        }
    }

    /** Closes the app and launches it again with the same home and language, without seed or arguments. */
    fun relaunch() = rule.restart(seed = null, args = emptyList())

    /** Same as [relaunch], with a new seed and launch arguments. */
    fun restart(seed: Seed, args: List<String> = emptyList()) = rule.restart(seed, args)

    /** Runs a second app process on the same home without attaching to it and returns its exit code. */
    fun launchSecondInstance(args: List<String>): Int = rule.runSecondInstance(args)

    /** The next import or export file dialog answers with [file], or cancels when it is null. */
    fun respondToFileDialog(file: File?) {
        rule.home.fileDialogResponse.writeText(file?.absolutePath?.plus("\n").orEmpty())
    }

    fun openedUris(): List<String> =
        rule.home.openedUriLog.takeIf(File::exists)?.readLines()?.filter(String::isNotBlank).orEmpty()

    fun awaitOpenedUri(predicate: (String) -> Boolean): String =
        await("an opened URI") { openedUris().firstOrNull(predicate) }

    fun fixtureUrl(path: String): String = rule.fixtureUrl(path)

    /** Copies an OPML fixture into the home with its feed URLs pointing at the fixture server. */
    fun fixtureOpml(name: String): File {
        val source = File(repoRoot, "e2e/fixtures/opml/$name").readText()
        val rewritten = source
            .replace(Regex("""xmlUrl="[^"]*""""), """xmlUrl="${fixtureUrl(FIXTURE_FEED_PATH)}"""")
            .replace(Regex("""htmlUrl="[^"]*""""), """htmlUrl="${fixtureUrl("/")}"""")
        return File(rule.home.dir, name).apply { writeText(rewritten) }
    }

    fun idle() {
        automator.waitForIdle()
    }

    fun treeHead(): String = runCatching {
        automator.printTree().lineSequence().take(TREE_LINES).joinToString("\n")
    }.getOrElse { "tree unavailable: $it" }

    fun <T : Any> await(what: String, timeoutMs: Long = AWAIT_TIMEOUT_MS, block: () -> T?): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            val value = runCatching(block).onFailure { lastError = it }.getOrNull()
            if (value != null) return value
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("Timed out after $timeoutMs ms waiting for $what\n${treeHead()}", lastError)
    }

    private companion object {
        const val TREE_LINES = 200
        const val DRAG_STEPS = 20
        const val DRAG_DURATION_MS = 600L
        const val CLICK_RETRIES = 5
        const val CLICK_RETRY_DELAY_MS = 300L
    }
}

/** Off-screen nodes in lazy and scrolled containers report isVisible with 1x1 bounds at the window origin. */
val NodeSnapshotDto.isOnScreen: Boolean
    get() = isVisible && bounds.width > 1 && bounds.height > 1

/** A mouse drag between two screen points, for swipe gestures. */
fun Flow.swipe(fromX: Int, fromY: Int, toX: Int, toY: Int, steps: Int = 20, durationMs: Long = 400) {
    automator.swipe(fromX, fromY, toX, toY, steps, durationMs)
    idle()
}
