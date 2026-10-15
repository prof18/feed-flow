package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.home.DesktopFeedTreeProjection
import com.prof18.feedflow.desktop.home.DesktopFeedTreeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Coordinates focus across lazy tree rows without changing the article filter. */
internal class DesktopFeedTreeKeyboard(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
) {
    private var focusedKey: String? = null
    var pendingFocusKey by mutableStateOf<String?>(null)
        private set

    fun focused(key: String) {
        focusedKey = key
        if (pendingFocusKey == key) pendingFocusKey = null
    }

    fun handle(
        event: KeyEvent,
        rows: List<DesktopFeedTreeKeyboardRow>,
        treeState: DesktopFeedTreeState,
        onRangeFocus: (String, Boolean) -> Unit,
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val index = rows.indexOfFirst { it.key == focusedKey }
        val row = rows.getOrNull(index) ?: return false
        return when (event.key) {
            Key.DirectionDown, Key.DirectionUp -> {
                val delta = if (event.key == Key.DirectionDown) 1 else -1
                val next = rows[(index + delta).coerceIn(rows.indices)]
                focus(next)
                if (event.isShiftPressed && next.key.startsWith("feed:")) {
                    val toggle = if (getDesktopOS().isMacOs()) event.isMetaPressed else event.isCtrlPressed
                    onRangeFocus(next.key.removePrefix("feed:"), toggle)
                }
                true
            }
            Key.DirectionLeft -> {
                val section = row.sectionKey ?: row.parentKey
                if (section != null) {
                    rows.firstOrNull { it.key == section }?.let(::focus)
                    treeState.setExpanded(section, false)
                }
                section != null
            }
            Key.DirectionRight -> {
                if (row.sectionKey != null) {
                    if (treeState.isExpanded(row.sectionKey)) {
                        rows.getOrNull(index + 1)?.takeIf { it.parentKey == row.sectionKey }?.let(::focus)
                    } else {
                        treeState.setExpanded(row.sectionKey, true)
                    }
                }
                row.sectionKey != null
            }
            else -> false
        }
    }

    private fun focus(row: DesktopFeedTreeKeyboardRow) {
        scope.launch {
            listState.scrollToItem(row.lazyIndex)
            pendingFocusKey = row.key
        }
    }
}

@Composable
internal fun desktopTreeRowFocusModifier(key: String, keyboard: DesktopFeedTreeKeyboard): Modifier {
    val requester = remember(key) { FocusRequester() }
    LaunchedEffect(keyboard.pendingFocusKey) {
        if (keyboard.pendingFocusKey == key) requester.requestFocus()
    }
    return Modifier.focusRequester(requester).onFocusChanged { if (it.isFocused) keyboard.focused(key) }
}

internal fun desktopTreeKeyboardRows(
    tree: DesktopFeedTreeProjection,
    state: DesktopFeedTreeState,
    retainedCollapsedSections: Set<String> = emptySet(),
): List<DesktopFeedTreeKeyboardRow> = buildList {
    var lazyIndex = 0
    listOf("timeline", "read", "bookmarks").forEach { add(DesktopFeedTreeKeyboardRow(it, lazyIndex++)) }
    lazyIndex++ // Source heading and its actions are not focusable tree rows.
    if (tree.pinned.isNotEmpty()) {
        lazyIndex += 2 // Divider and pinned heading are not focusable rows.
        tree.pinned.forEach { add(DesktopFeedTreeKeyboardRow("feed:${it.feedSource.id}", lazyIndex++)) }
    }
    tree.sections.forEach { section ->
        add(DesktopFeedTreeKeyboardRow(section.id, lazyIndex++, sectionKey = section.id))
        if (state.isExpanded(section.id)) {
            section.visibleFeeds.forEach {
                add(DesktopFeedTreeKeyboardRow("feed:${it.feedSource.id}", lazyIndex++, parentKey = section.id))
            }
        } else if (section.id in retainedCollapsedSections) {
            lazyIndex += section.visibleFeeds.size
        }
    }
}

internal data class DesktopFeedTreeKeyboardRow(
    val key: String,
    val lazyIndex: Int,
    val sectionKey: String? = null,
    val parentKey: String? = null,
)
