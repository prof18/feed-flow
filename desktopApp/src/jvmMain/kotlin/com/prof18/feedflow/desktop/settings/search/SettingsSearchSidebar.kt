package com.prof18.feedflow.desktop.settings.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.rememberDesktopSidebarItemStyle
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip

internal const val SETTINGS_SEARCH_FIELD_TAG = "settings-search-field"
internal const val SETTINGS_SEARCH_CLEAR_TAG = "settings-search-clear"
internal const val SETTINGS_SEARCH_RESULTS_TAG = "settings-search-results"

@Composable
internal fun SettingsSearchField(
    state: TextFieldState,
    focusRequester: FocusRequester,
    onMoveActive: (delta: Int) -> Unit,
    onOpenActive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    TextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .testTag(SETTINGS_SEARCH_FIELD_TAG)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        onMoveActive(1)
                        true
                    }
                    Key.DirectionUp -> {
                        onMoveActive(-1)
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        onOpenActive()
                        true
                    }
                    Key.Escape -> state.clearSettingsSearch()
                    else -> false
                }
            },
        placeholder = { Text(strings.desktopSettingsSearchPlaceholder) },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = JewelTheme.globalColors.text.info,
                modifier = Modifier.padding(end = 6.dp).size(16.dp),
            )
        },
        trailingIcon = if (state.text.isNotEmpty()) {
            {
                SettingsSearchClearButton(
                    onClick = {
                        state.clearSettingsSearch()
                        focusRequester.requestFocus()
                    },
                )
            }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsSearchClearButton(onClick: () -> Unit) {
    val description = LocalFeedFlowStrings.current.desktopSearchClearButton
    Tooltip(tooltip = { Text(description) }) {
        IconButton(
            onClick = onClick,
            // Mouse-only: it sits inside the field, whose key handler would take Enter from it; Escape clears instead.
            modifier = Modifier
                .testTag(SETTINGS_SEARCH_CLEAR_TAG)
                .focusProperties { canFocus = false }
                .semantics { contentDescription = description },
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = null,
                tint = JewelTheme.globalColors.text.info,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
internal fun SettingsSearchResults(
    results: List<SettingsSearchEntry>,
    activeIndex: Int,
    onOpen: (SettingsSearchEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (results.isEmpty()) {
        Text(
            LocalFeedFlowStrings.current.desktopSettingsSearchNoResults,
            color = JewelTheme.globalColors.text.info,
            modifier = modifier.testTag(SETTINGS_SEARCH_RESULTS_TAG).padding(horizontal = 8.dp, vertical = 6.dp),
        )
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(results, activeIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.none { it.index == activeIndex }) listState.scrollToItem(activeIndex)
    }
    val rowStyle = rememberDesktopSidebarItemStyle()
    val activeBackground = rowStyle.colors.backgroundFor(ListItemState(isSelected = true, isActive = false)).value
    LazyColumn(modifier = modifier.testTag(SETTINGS_SEARCH_RESULTS_TAG), state = listState) {
        itemsIndexed(results, key = { index, entry -> "$index:${entry.category}:${entry.title}" }) { index, entry ->
            val isActive = index == activeIndex
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Focus stays in the search field, so the keyboard-active result is exposed as selected and
                    // announced politely when it changes.
                    .semantics(mergeDescendants = true) {
                        selected = isActive
                        if (isActive) liveRegion = LiveRegionMode.Polite
                    }
                    .heightIn(min = 36.dp)
                    .background(
                        color = if (isActive) activeBackground else Color.Transparent,
                        shape = RoundedCornerShape(DesktopDesignTokens.buttonCornerSize),
                    )
                    .clickable(role = Role.Button) { onOpen(entry) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(entry.title, maxLines = 2)
                if (entry.title != entry.pageTitle) {
                    Text(
                        entry.pageTitle,
                        style = DesktopDesignTokens.metadataText,
                        color = JewelTheme.globalColors.text.info,
                    )
                }
            }
        }
    }
}

// Clears a non-empty query and reports whether it did, so Escape on an empty query still closes the window.
internal fun TextFieldState.clearSettingsSearch(): Boolean {
    if (text.isEmpty()) return false
    setTextAndPlaceCursorAtEnd("")
    return true
}

internal fun isSettingsSearchShortcut(
    key: Key,
    type: KeyEventType,
    meta: Boolean,
    ctrl: Boolean,
    isMacOs: Boolean,
): Boolean = type == KeyEventType.KeyDown && key == Key.F && if (isMacOs) meta else ctrl
