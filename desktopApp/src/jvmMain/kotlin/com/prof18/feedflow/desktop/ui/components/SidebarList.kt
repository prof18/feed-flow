package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.home.DesktopFeedSelectionState
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.foundation.lazy.SelectableColumnKeybindings
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListKey
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.lazy.SingleSelectionLazyColumn
import org.jetbrains.jewel.foundation.lazy.rememberSingleSelectionLazyListState
import org.jetbrains.jewel.foundation.lazy.tree.DefaultSelectableLazyColumnEventAction
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.styling.dark
import org.jetbrains.jewel.intui.standalone.styling.light
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.SimpleListItem
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.SimpleListItemColors
import org.jetbrains.jewel.ui.component.styling.SimpleListItemStyle
import org.jetbrains.jewel.ui.icon.IconKey

/** A single keyboard focus target; selection navigation never activates the article filter. */
@Composable
internal fun SidebarList(
    entries: ImmutableList<SidebarEntry>,
    activeKey: SidebarKey?,
    onActivate: (SidebarKey) -> Unit,
    modifier: Modifier = Modifier,
    onSelectionChanged: (Set<String>) -> Unit = {},
    onFocusedKeyChanged: (SidebarKey?) -> Unit = {},
    onExpansionChange: (SidebarKey, Boolean) -> Unit = { _, _ -> },
    leadingIcon: (SidebarKey) -> IconKey? = { null },
    sectionHeader: (@Composable () -> Unit)? = null,
    trailingContent: @Composable (SidebarKey) -> Unit = {},
) {
    val listState = rememberSingleSelectionLazyListState(initialSelectedKey = activeKey)
    val itemStyle = rememberDesktopSidebarItemStyle()
    var selection by remember { mutableStateOf(DesktopFeedSelectionState()) }
    val selectedFeedIds = selection.selectedIds
    val latestEntries by rememberUpdatedState(entries)
    val latestActiveKey by rememberUpdatedState(activeKey)
    val latestActivate by rememberUpdatedState(onActivate)
    val latestSelectionChanged by rememberUpdatedState(onSelectionChanged)
    val latestFocusChanged by rememberUpdatedState(onFocusedKeyChanged)
    val latestExpansionChange by rememberUpdatedState(onExpansionChange)

    fun commit(key: SidebarKey?, toggle: Boolean, range: Boolean) {
        selection = selectSidebarKey(key, toggle, range, latestEntries, latestActiveKey, selection)
        latestSelectionChanged(selection.selectedIds)
        key.takeUnless { toggle || range }?.let(latestActivate)
    }

    SideEffect(listState.selectedKeys) {
        latestFocusChanged(listState.selectedKeys.firstOrNull() as? SidebarKey)
    }
    SideEffect(activeKey) {
        if (activeKey != null && entries.any { it.key == activeKey }) {
            listState.selectedKeys = setOf(activeKey)
        }
    }
    SideEffect(entries.map { it.key }) {
        val validKeys = entries.mapTo(mutableSetOf()) { it.key }
        if (listState.selectedKeys.none { it in validKeys }) {
            listState.selectedKeys = setOfNotNull(
                activeKey?.takeIf { it in validKeys } ?: entries.firstOrNull()?.key,
            )
        }
        val remainingIds = selectedFeedIds.intersect(
            entries.mapNotNullTo(mutableSetOf()) {
                (it.key as? SidebarKey.Feed)?.id
            },
        )
        if (remainingIds != selectedFeedIds) {
            selection = selection.reconcile(remainingIds)
            latestSelectionChanged(remainingIds)
        }
    }
    val pointerActions = remember {
        object : DefaultSelectableLazyColumnEventAction() {
            override fun handlePointerEventPress(
                pointerEvent: PointerEvent,
                keybindings: SelectableColumnKeybindings,
                selectableLazyListState: SelectableLazyListState,
                selectionMode: SelectionMode,
                allKeys: List<SelectableLazyListKey>,
                key: Any,
            ) {
                if (!pointerEvent.buttons.isPrimaryPressed || pointerEvent.changes.any { it.isConsumed }) {
                    return
                }
                super.handlePointerEventPress(
                    pointerEvent,
                    keybindings,
                    selectableLazyListState,
                    selectionMode,
                    allKeys,
                    key,
                )
                val modifiers = pointerEvent.keyboardModifiers
                commit(
                    key as? SidebarKey,
                    if (getDesktopOS().isMacOs()) modifiers.isMetaPressed else modifiers.isCtrlPressed,
                    modifiers.isShiftPressed,
                )
            }
        }
    }
    SingleSelectionLazyColumn(
        state = listState,
        modifier = modifier
            .testTag("sidebar-list")
            .onPreviewKeyEvent { event ->
                handleSidebarRangeKeyEvent(event, entries, listState.selectedKeys) { next, toggle ->
                    listState.selectedKeys = setOf(next)
                    commit(next, toggle, true)
                }
            }
            .onKeyEvent { event ->
                handleSidebarKeyEvent(
                    event = event,
                    selectedKey = listState.selectedKeys.firstOrNull() as? SidebarKey,
                    onActivate = ::commit,
                    onExpansionChange = latestExpansionChange,
                )
            },
        pointerEventActions = pointerActions,
    ) {
        entries.forEachIndexed { index, entry ->
            if (sectionHeader != null && index == PRIMARY_NAV_ITEM_COUNT) {
                item(key = SIDEBAR_SECTION_HEADER_KEY, contentType = SIDEBAR_SECTION_HEADER_KEY, selectable = false) {
                    sectionHeader()
                }
            }
            item(key = entry.key, contentType = entry.key::class) {
                val itemState = ListItemState(
                    isSelected = isSelected ||
                        (entry.key as? SidebarKey.Feed)?.id in selectedFeedIds,
                    isActive = isActive,
                )
                SimpleListItem(
                    state = itemState,
                    modifier = Modifier
                        .testTag("sidebar-${entry.key}")
                        .padding(
                            start = if (entry.key is SidebarKey.Feed && !entry.isPinned) 12.dp else 0.dp,
                        ),
                    icon = leadingIcon(entry.key),
                    style = itemStyle,
                    height = 36.dp,
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = entry.label.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        trailingContent(entry.key)
                    }
                }
            }
        }
        if (sectionHeader != null && entries.size <= PRIMARY_NAV_ITEM_COUNT) {
            item(key = SIDEBAR_SECTION_HEADER_KEY, contentType = SIDEBAR_SECTION_HEADER_KEY, selectable = false) {
                sectionHeader()
            }
        }
    }
}

@Composable
internal fun rememberDesktopSidebarItemStyle(): SimpleListItemStyle {
    val selectedBackground = JewelTheme.globalColors.text.normal.copy(alpha = INACTIVE_SELECTION_ALPHA)
    val isDark = JewelTheme.isDark
    return remember(isDark, selectedBackground) {
        if (isDark) {
            SimpleListItemStyle.dark(
                colors = SimpleListItemColors.dark(backgroundSelected = selectedBackground),
            ).withRoundedSelectionCorners()
        } else {
            SimpleListItemStyle.light(
                colors = SimpleListItemColors.light(backgroundSelected = selectedBackground),
            ).withRoundedSelectionCorners()
        }
    }
}

private const val PRIMARY_NAV_ITEM_COUNT = 3
private const val SIDEBAR_SECTION_HEADER_KEY = "feed-sources-header"
private const val INACTIVE_SELECTION_ALPHA = 0.08f

private fun handleSidebarKeyEvent(
    event: KeyEvent,
    selectedKey: SidebarKey?,
    onActivate: (SidebarKey?, Boolean, Boolean) -> Unit,
    onExpansionChange: (SidebarKey, Boolean) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val isSection = selectedKey is SidebarKey.Category || selectedKey == SidebarKey.Uncategorized
    val isExpansionKey = event.key == Key.DirectionRight || event.key == Key.DirectionLeft
    return when {
        isSection && selectedKey != null && isExpansionKey -> {
            onExpansionChange(selectedKey, event.key == Key.DirectionRight)
            true
        }
        event.key == Key.Enter -> {
            onActivate(
                selectedKey,
                if (getDesktopOS().isMacOs()) event.isMetaPressed else event.isCtrlPressed,
                event.isShiftPressed,
            )
            true
        }
        else -> false
    }
}

private fun selectSidebarKey(
    key: SidebarKey?,
    toggle: Boolean,
    range: Boolean,
    entries: List<SidebarEntry>,
    activeKey: SidebarKey?,
    selection: DesktopFeedSelectionState,
): DesktopFeedSelectionState = when {
    key is SidebarKey.Feed -> selection.select(
        id = key.id,
        visibleIds = entries.mapNotNull { (it.key as? SidebarKey.Feed)?.id },
        toggle = toggle,
        range = range,
        activeId = (activeKey as? SidebarKey.Feed)?.id,
    )
    !toggle && !range -> DesktopFeedSelectionState()
    else -> selection
}

private fun handleSidebarRangeKeyEvent(
    event: KeyEvent,
    entries: List<SidebarEntry>,
    selectedKeys: Set<Any>,
    onNavigate: (SidebarKey, Boolean) -> Unit,
): Boolean {
    val direction = when (event.key) {
        Key.DirectionDown -> 1
        Key.DirectionUp -> -1
        else -> 0
    }
    if (event.type != KeyEventType.KeyDown || !event.isShiftPressed || direction == 0 || entries.isEmpty()) return false
    val current = entries.indexOfFirst { it.key in selectedKeys }.coerceAtLeast(0)
    val next = entries[(current + direction).coerceIn(entries.indices)].key
    onNavigate(next, if (getDesktopOS().isMacOs()) event.isMetaPressed else event.isCtrlPressed)
    return true
}
