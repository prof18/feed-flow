package com.prof18.feedflow.desktop.e2e.harness

import dev.sebastiano.spectre.agent.transport.NodeSnapshotDto
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

private const val MAIN_SURFACE = "window:1:"
private const val SCROLL_STEP = 5
private const val MAX_SCROLLS = 40
private const val CLICK_ATTEMPTS = 3
private const val RETRY_WAIT_MS = 3_000L

/** Nodes of a window or dialog, by title (dialogs are popups in windows()). */
fun Flow.nodesIn(windowTitle: String): List<NodeSnapshotDto> {
    val window = awaitWindow(windowTitle)
    return nodes().filter { it.key.startsWith("${window.surfaceId}:") }
}

fun Flow.awaitTextIn(windowTitle: String, text: String, exact: Boolean = true): NodeSnapshotDto =
    await("text '$text' in window '$windowTitle'") {
        nodesIn(windowTitle).firstOrNull { node -> node.isOnScreen && node.matches(text, exact) }
    }

fun Flow.clickIn(windowTitle: String, text: String, exact: Boolean = true) =
    click(awaitTextIn(windowTitle, text, exact))

fun Flow.awaitTagIn(windowTitle: String, tag: String): NodeSnapshotDto =
    await("tag '$tag' in window '$windowTitle'") { nodesIn(windowTitle).firstOrNull { it.testTag == tag } }

fun Flow.awaitDescriptionIn(windowTitle: String, description: String): NodeSnapshotDto =
    await("content description '$description' in window '$windowTitle'") {
        nodesIn(windowTitle).firstOrNull { it.isOnScreen && description in it.contentDescriptions }
    }

/** A sidebar entry (Timeline, Read, Bookmarks, a category or a feed). */
fun Flow.sidebarRow(name: String): NodeSnapshotDto =
    await("sidebar row '$name'") {
        nodes().firstOrNull { it.role == "Tab" && it.texts.firstOrNull() == name && it.key.startsWith(MAIN_SURFACE) }
    }

/** Clicks a sidebar row and waits until it is selected, clicking again if the first click was lost. */
fun Flow.clickSidebar(name: String) {
    repeat(CLICK_ATTEMPTS) {
        click(sidebarRow(name))
        val selected = runCatching {
            await("sidebar row '$name' to be selected", RETRY_WAIT_MS) { sidebarRow(name).takeIf { it.isSelected } }
        }.isSuccess
        if (selected) return
    }
    error("sidebar row '$name' did not get selected\n${treeHead()}")
}

/** The unread count shown next to a sidebar entry, or null when it shows none. */
fun Flow.sidebarCount(name: String): String? = sidebarRow(name).texts.drop(1).firstOrNull()

/** Opens a sidebar feed or category menu with Shift+F10 on the focused row (FACT context-menu). */
fun Flow.openSidebarMenu(name: String) {
    clickSidebar(name)
    repeat(CLICK_ATTEMPTS) {
        key(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)
        val opened = runCatching {
            await("sidebar menu of '$name'", RETRY_WAIT_MS) {
                nodes().firstOrNull { it.isOnScreen && it.isInMainPopup }
            }
        }.isSuccess
        if (opened) return
    }
}

/** Clicks an entry of an open popup menu in the main window. */
fun Flow.clickMenuItem(text: String) = click(await("menu item '$text'") { menuItem(text) })

/** Opens one entry of the sidebar's Add feed menu (Add feed, Feed Suggestions, Import feed from OPML). */
fun Flow.openAddMenuItem(item: String, addButtonDescription: String = strings.addFeed) {
    repeat(CLICK_ATTEMPTS) {
        click(
            await("sidebar add button") {
                nodes().firstOrNull {
                    addButtonDescription in it.contentDescriptions &&
                        it.key.startsWith("${MAIN_SURFACE}0:") &&
                        it.isOnScreen
                }
            },
        )
        if (runCatching { await("menu item '$item'", RETRY_WAIT_MS) { menuItem(item) } }.isSuccess) {
            clickMenuItem(item)
            return
        }
    }
    clickMenuItem(item)
}

private fun Flow.menuItem(text: String): NodeSnapshotDto? =
    nodes().lastOrNull { node ->
        node.isOnScreen && node.texts.firstOrNull() == text && node.isInMainPopup
    }

fun Flow.openAddFeed() {
    openAddMenuItem(strings.addFeed)
    awaitWindow(strings.addFeed)
}

/** Opens Settings (through the sidebar's import entry, the only in-window route) on the given pane. */
fun Flow.openSettings(pane: String) {
    openAddMenuItem(strings.importFeedButton)
    awaitWindow(strings.settingsTitle)
    clickIn(strings.settingsTitle, pane)
}

fun Flow.closeWindow(title: String) {
    awaitWindow(title)
    key(KeyEvent.VK_ESCAPE)
    awaitWindowGone(title)
}

fun Flow.articleRow(id: String): String = "article_row_${id.replace(Regex("[^A-Za-z0-9_]"), "_")}"

/** Visible article rows, top to bottom. */
fun Flow.visibleArticleRows(): List<NodeSnapshotDto> =
    nodesWithTagPrefix("article_row_")
        .filter { it.isOnScreen && it.key.startsWith(MAIN_SURFACE) }
        .sortedBy { it.bounds.y }

fun Flow.isArticleShown(title: String): Boolean = visibleArticleRows().any { title in it.texts }

/** Scrolls the article list down until the node with [tag] is on screen. */
fun Flow.scrollToTag(tag: String): NodeSnapshotDto {
    repeat(MAX_SCROLLS) {
        nodes().firstOrNull { it.testTag == tag && it.isOnScreen && it.bounds.height > MIN_ROW_HEIGHT }
            ?.let { return it }
        val anchor = visibleArticleRows().lastOrNull() ?: awaitTagPrefix("article_row_").first()
        scroll(anchor, SCROLL_STEP)
    }
    return awaitTag(tag)
}

/** Scrolls the article list down until a node showing [text] is on screen. */
fun Flow.scrollToText(text: String, exact: Boolean = true): NodeSnapshotDto {
    repeat(MAX_SCROLLS) {
        nodes().firstOrNull { it.isOnScreen && it.matches(text, exact) && it.key.startsWith(MAIN_SURFACE) }
            ?.let { return it }
        val anchor = visibleArticleRows().lastOrNull() ?: return awaitText(text, exact)
        scroll(anchor, SCROLL_STEP)
    }
    return awaitText(text, exact)
}

fun Flow.readerButton(description: String): NodeSnapshotDto =
    await("reader button '$description'") {
        nodes().lastOrNull { it.isOnScreen && description in it.contentDescriptions && it.key.startsWith(MAIN_SURFACE) }
    }

fun Flow.clickReaderButton(description: String) = click(readerButton(description))

fun Flow.hasButton(description: String): Boolean =
    nodes().any { it.isOnScreen && description in it.contentDescriptions }

/** Types into the focused text field. */
fun Flow.typeIntoFocused(text: String) {
    val field = await("a focused text field") { nodes().firstOrNull { it.isFocused && it.editableText != null } }
    typeInto(field, text)
}

/** Replaces the text of a field: select all, then type. */
fun Flow.replaceText(node: NodeSnapshotDto, text: String) {
    click(node)
    key(KeyEvent.VK_A, InputEvent.META_DOWN_MASK)
    key(KeyEvent.VK_BACK_SPACE)
    typeInto(node, text)
}

fun NodeSnapshotDto.matches(text: String, exact: Boolean): Boolean =
    texts.any { if (exact) it == text else it.contains(text) } ||
        (editableText?.let { if (exact) it == text else it.contains(text) } ?: false)

private const val MIN_ROW_HEIGHT = 30

/** The reader pane shows [title] as its heading (article rows hold the title among several texts). */
fun Flow.awaitReaderTitle(title: String): NodeSnapshotDto =
    await("reader title '$title'") {
        nodes().firstOrNull { it.isOnScreen && it.texts == listOf(title) && it.key.startsWith(MAIN_SURFACE) }
    }

fun Flow.openArticle(id: String) {
    click(scrollToTag(articleRow(id)))
}

/** Scrolls a dialog window's content until [find] returns an on-screen node. */
fun Flow.scrollInWindow(windowTitle: String, what: String, find: (NodeSnapshotDto) -> Boolean): NodeSnapshotDto {
    repeat(MAX_SCROLLS) {
        val nodes = nodesIn(windowTitle)
        nodes.firstOrNull { it.isOnScreen && find(it) }?.let { return it }
        val anchor = nodes
            .filter { it.isOnScreen && it.role == null && it.texts.isNotEmpty() }
            .maxByOrNull { it.bounds.y }
            ?: return await(what) { nodesIn(windowTitle).firstOrNull { it.isOnScreen && find(it) } }
        scroll(anchor, SCROLL_STEP)
    }
    return await(what) { nodesIn(windowTitle).firstOrNull { it.isOnScreen && find(it) } }
}

/** A switch row in a dialog, found by its title. */
fun Flow.switchIn(windowTitle: String, title: String): NodeSnapshotDto =
    scrollInWindow(windowTitle, "switch '$title'") { it.role == "Switch" && it.texts.firstOrNull() == title }

fun Flow.isSwitchOn(windowTitle: String, title: String): Boolean = switchIn(windowTitle, title).toggleableState == "On"

fun Flow.setSwitch(windowTitle: String, title: String, on: Boolean) {
    if (isSwitchOn(windowTitle, title) != on) click(switchIn(windowTitle, title))
    await("switch '$title' to be ${if (on) "on" else "off"}") { Unit.takeIf { isSwitchOn(windowTitle, title) == on } }
}

/** A dropdown in a dialog, found by its label (content description). */
fun Flow.dropdownIn(windowTitle: String, label: String): NodeSnapshotDto =
    scrollInWindow(windowTitle, "dropdown '$label'") { it.role == "DropdownList" && label in it.contentDescriptions }

fun Flow.choose(windowTitle: String, label: String, option: String, expectSelected: Boolean = true) {
    click(dropdownIn(windowTitle, label))
    val window = awaitWindow(windowTitle)
    click(
        await("option '$option' of '$label'") {
            nodes().firstOrNull {
                it.isOnScreen && it.role == "RadioButton" && it.texts.firstOrNull() == option &&
                    it.key.startsWith("${window.surfaceId}:") && !it.key.startsWith("${window.surfaceId}:0:")
            }
        },
    )
    if (expectSelected) {
        await("'$label' to show '$option'") { Unit.takeIf { option in dropdownIn(windowTitle, label).texts } }
    }
}

fun Flow.articleImage(id: String): String = "article_image_${id.replace(Regex("[^A-Za-z0-9_]"), "_")}"

/** Clicks [target] until [done] holds, for controls whose click is sometimes lost while they recompose. */
fun Flow.clickUntil(what: String, target: () -> NodeSnapshotDto, done: () -> Boolean) {
    repeat(CLICK_ATTEMPTS) {
        click(target())
        if (runCatching { await(what, RETRY_WAIT_MS) { Unit.takeIf { done() } } }.isSuccess) return
    }
    await(what) { Unit.takeIf { done() } }
}

/** Clicks the reader button [from] until the reader shows the button [to] instead. */
fun Flow.toggleReaderButton(from: String, to: String) =
    clickUntil("reader button '$to'", { readerButton(from) }, { hasButton(to) })

/** Popup menus of the main window live in its layers 1 and up. */
private val NodeSnapshotDto.isInMainPopup: Boolean
    get() = key.startsWith(MAIN_SURFACE) && !key.startsWith("${MAIN_SURFACE}0:")
