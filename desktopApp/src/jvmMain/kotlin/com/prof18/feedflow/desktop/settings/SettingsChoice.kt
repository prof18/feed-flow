package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.modifier.onMove
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.ComboBox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.PopupManager
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.component.styling.ComboBoxMetrics
import org.jetbrains.jewel.ui.component.styling.ComboBoxStyle
import org.jetbrains.jewel.ui.component.styling.LocalPopupContainerStyle
import org.jetbrains.jewel.ui.component.styling.PopupContainerMetrics
import org.jetbrains.jewel.ui.component.styling.PopupContainerStyle
import org.jetbrains.jewel.ui.theme.comboBoxStyle
import org.jetbrains.jewel.ui.theme.popupContainerStyle
import org.jetbrains.jewel.ui.theme.simpleListItemStyle

@Composable
internal fun <T : Any> SettingsChoice(
    title: String,
    choices: ImmutableList<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    val fontScale = LocalDensity.current.fontScale
    val inSection = LocalInSettingsSection.current
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .settingsRowSeparator()
            .padding(horizontal = if (inSection) 14.dp else 4.dp, vertical = 8.dp),
    ) {
        val stacked = maxWidth < 440.dp || fontScale > INLINE_MAX_FONT_SCALE
        val controlWidth = if (stacked) maxWidth else 220.dp
        val label: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title)
                if (supportingText != null) {
                    Text(
                        supportingText,
                        style = DesktopDesignTokens.metadataText,
                        color = JewelTheme.globalColors.text.info,
                    )
                }
            }
        }
        val control: @Composable () -> Unit = {
            SettingsChoiceDropdown(
                title = title,
                choices = choices,
                selected = selected,
                onSelected = onSelected,
                enabled = enabled,
                maxPopupWidth = maxWidth.coerceAtMost(440.dp),
                modifier = Modifier.width(controlWidth),
            )
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                label()
                control()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { label() }
                control()
            }
        }
    }
}

@OptIn(ExperimentalJewelApi::class, ExperimentalFoundationApi::class)
@Composable
private fun <T : Any> SettingsChoiceDropdown(
    title: String,
    choices: ImmutableList<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    enabled: Boolean,
    maxPopupWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnSelected by rememberUpdatedState(onSelected)
    val selectedIndex = choices.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val selectedLabel = choices.getOrNull(selectedIndex)?.second.orEmpty()
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    var highlightedIndex by remember { mutableIntStateOf(selectedIndex) }
    val focusRequester = remember { FocusRequester() }
    val popupManager = remember {
        PopupManager(onPopupVisibleChange = { visible ->
            if (visible) highlightedIndex = currentSelectedIndex
        })
    }
    val popupVisible by popupManager.isPopupVisible
    val canSelect = enabled && choices.isNotEmpty()
    LaunchedEffect(canSelect) {
        if (!canSelect) popupManager.setPopupVisible(false)
    }
    fun commitChoice(index: Int) {
        if (latestEnabled) {
            choices.getOrNull(index)?.first?.let { if (it != selected) latestOnSelected(it) }
        }
        popupManager.setPopupVisible(false)
    }
    val base = JewelTheme.comboBoxStyle
    val popup = JewelTheme.popupContainerStyle
    val labelHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 12.dp
    val style = remember(base, labelHeight) {
        val metrics = base.metrics
        ComboBoxStyle(
            colors = base.colors,
            icons = base.icons,
            metrics = ComboBoxMetrics(
                arrowAreaSize = metrics.arrowAreaSize,
                minSize = DpSize(
                    width = metrics.minSize.width,
                    height = labelHeight.coerceAtLeast(DesktopDesignTokens.settingsRowMinHeight),
                ),
                cornerSize = CornerSize(DesktopDesignTokens.buttonCornerSize),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                popupContentPadding = PaddingValues(6.dp),
                borderWidth = metrics.borderWidth,
                maxPopupHeight = metrics.maxPopupHeight,
                maxPopupRowCount = metrics.maxPopupRowCount,
            ),
        )
    }
    val popupStyle = remember(popup) {
        val metrics = popup.metrics
        PopupContainerStyle(
            isDark = popup.isDark,
            colors = popup.colors,
            metrics = PopupContainerMetrics(
                cornerSize = CornerSize(12.dp),
                menuMargin = metrics.menuMargin,
                contentPadding = metrics.contentPadding,
                offset = metrics.offset,
                shadowSize = metrics.shadowSize,
                borderWidth = metrics.borderWidth,
            ),
        )
    }
    CompositionLocalProvider(LocalPopupContainerStyle provides popupStyle) {
        ComboBox(
            modifier = modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (!canSelect || !popupVisible || it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    handleChoiceKey(
                        key = it.key,
                        lastIndex = choices.lastIndex,
                        onHighlight = { index -> highlightedIndex = index },
                        onCommit = { commitChoice(highlightedIndex) },
                    )
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = title
                    stateDescription = selectedLabel
                    role = Role.DropdownList
                    if (!canSelect) disabled()
                    onClick {
                        if (latestEnabled && choices.isNotEmpty()) {
                            focusRequester.requestFocus()
                            popupManager.togglePopupVisibility()
                            true
                        } else {
                            false
                        }
                    }
                },
            enabled = canSelect,
            style = style,
            maxPopupHeight = 320.dp,
            maxPopupWidth = maxPopupWidth,
            popupManager = popupManager,
            onArrowDownPress = { highlightedIndex = (highlightedIndex + 1).coerceAtMost(choices.lastIndex) },
            onArrowUpPress = { highlightedIndex = (highlightedIndex - 1).coerceAtLeast(0) },
            labelContent = {
                Tooltip(tooltip = { Text(selectedLabel) }) {
                    Text(
                        selectedLabel,
                        modifier = Modifier.padding(style.metrics.contentPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            popupContent = {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(style.metrics.popupContentPadding)) {
                    choices.forEachIndexed { index, item ->
                        SettingsChoiceOption(
                            label = item.second,
                            selected = item.first == selected,
                            highlighted = index == highlightedIndex,
                            enabled = canSelect,
                            onHover = { highlightedIndex = index },
                            onClick = { commitChoice(index) },
                        )
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsChoiceOption(
    label: String,
    selected: Boolean,
    highlighted: Boolean,
    enabled: Boolean,
    onHover: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(highlighted) {
        if (highlighted) bringIntoViewRequester.bringIntoView()
    }
    val state = ListItemState(isSelected = highlighted, isActive = true)
    val itemStyle = JewelTheme.simpleListItemStyle
    val color = itemStyle.colors.contentFor(state).value.takeOrElse { JewelTheme.globalColors.text.normal }
    Row(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .background(
                itemStyle.colors.backgroundFor(state).value,
                RoundedCornerShape(DesktopDesignTokens.selectionCornerSize),
            )
            .onMove { onHover() }
            .semantics(mergeDescendants = true) {
                onClick {
                    if (enabled) onClick()
                    enabled
                }
            }
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), color = color)
        Box(Modifier.size(16.dp)) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = color)
            }
        }
    }
}

private const val INLINE_MAX_FONT_SCALE = 1.4f

private fun handleChoiceKey(
    key: Key,
    lastIndex: Int,
    onHighlight: (Int) -> Unit,
    onCommit: () -> Unit,
): Boolean {
    when (key) {
        Key.MoveHome -> onHighlight(0)
        Key.MoveEnd -> onHighlight(lastIndex)
        Key.Enter, Key.NumPadEnter -> onCommit()
        else -> return false
    }
    return true
}
