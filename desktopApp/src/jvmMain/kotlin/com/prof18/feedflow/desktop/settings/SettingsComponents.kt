package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.rememberScrollbarState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.desktop.ui.components.rememberDesktopSidebarItemStyle
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.lazy.SingleSelectionLazyColumn
import org.jetbrains.jewel.foundation.lazy.items
import org.jetbrains.jewel.foundation.lazy.rememberSingleSelectionLazyListState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.SimpleListItemStyle

@Composable
internal fun SettingsContent(
    initialCategory: DesktopSettingsCategory,
    modifier: Modifier = Modifier,
    content: @Composable (DesktopSettingsCategory) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val categories = listOf(
        DesktopSettingsCategory.GENERAL to strings.settingsAppTitle,
        DesktopSettingsCategory.ACCOUNTS to strings.settingsAccounts,
        DesktopSettingsCategory.FEED_LIST to strings.settingsFeedListTitle,
        DesktopSettingsCategory.READING to strings.settingsReadingBehavior,
        DesktopSettingsCategory.SYNC_STORAGE to strings.settingsSyncAndStorage,
        DesktopSettingsCategory.ABOUT to strings.settingsAboutAndSupport,
    )
    val categoryListState = rememberSingleSelectionLazyListState(initialSelectedKey = initialCategory)
    val categoryStyle = rememberDesktopSidebarItemStyle()
    val saveableStateHolder = rememberSaveableStateHolder()
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }

    LaunchedEffect(initialCategory) {
        categoryListState.selectedKeys = setOf(initialCategory)
        selectedCategory = initialCategory
    }
    LaunchedEffect(categoryListState.selectedKeys) {
        (categoryListState.selectedKeys.firstOrNull() as? DesktopSettingsCategory)?.let {
            selectedCategory = it
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val navigationWidth = if (maxWidth < NAVIGATION_COMPACT_BREAKPOINT) {
            NAVIGATION_MIN_WIDTH
        } else {
            NAVIGATION_WIDTH
        }
        Row(modifier = Modifier.fillMaxSize()) {
            SingleSelectionLazyColumn(
                state = categoryListState,
                modifier = Modifier
                    .testTag(SETTINGS_CATEGORIES_TAG)
                    .width(navigationWidth)
                    .fillMaxHeight()
                    .background(JewelTheme.globalColors.toolwindowBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                items(items = categories, key = { it.first }) { (_, label) ->
                    SettingsCategoryRow(
                        label = label,
                        state = ListItemState(isSelected = isSelected, isActive = isActive),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        style = categoryStyle,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
            ) {
                saveableStateHolder.SaveableStateProvider(selectedCategory) {
                    content(selectedCategory)
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    label: String,
    state: ListItemState,
    style: SimpleListItemStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                selected = state.isSelected
                isTraversalGroup = false
            }
            .heightIn(min = 40.dp)
            .padding(style.metrics.outerPadding)
            .background(
                color = style.colors.backgroundFor(state).value,
                shape = RoundedCornerShape(style.metrics.selectionBackgroundCornerSize),
            )
            .padding(style.metrics.innerPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = label, color = style.colors.contentFor(state).value)
    }
}

internal const val SETTINGS_CATEGORIES_TAG = "settings-categories"
private val NAVIGATION_WIDTH = 200.dp
private val NAVIGATION_MIN_WIDTH = 168.dp
private val NAVIGATION_COMPACT_BREAKPOINT = 800.dp

@Composable
internal fun SettingsGroup(title: String, modifier: Modifier = Modifier) {
    GroupHeader(
        text = title,
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp),
        textStyle = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    )
}

@Composable
internal fun SettingsScrollPane(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 12.dp),
            content = content,
        )
        if (scrollState.maxValue > 0) {
            FeedFlowVerticalScrollbar(rememberScrollbarState(scrollState))
        }
    }
}

@Composable
internal fun SettingsCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CheckboxRow(
            text = title,
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(start = 28.dp, end = 12.dp, bottom = 8.dp),
                color = JewelTheme.globalColors.text.info,
            )
        }
    }
}

@OptIn(ExperimentalJewelApi::class)
@Composable
internal fun <T : Any> JewelSettingsChoice(
    title: String,
    choices: ImmutableList<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val selectedIndex = choices.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnSelected by rememberUpdatedState(onSelected)
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GroupHeader(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            textStyle = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Medium),
        )
        if (choices.size <= SHORT_CHOICE_LIMIT) {
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                choices.forEach { (value, label) ->
                    RadioButtonRow(
                        text = label,
                        selected = value == selected,
                        enabled = enabled,
                        onClick = { if (latestEnabled) latestOnSelected(value) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
            }
        } else {
            ListComboBox(
                items = choices.map { it.second },
                selectedIndex = selectedIndex,
                onSelectedItemChange = { index ->
                    if (latestEnabled) choices.getOrNull(index)?.first?.let(latestOnSelected)
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = title },
                itemKeys = { index, _ -> choices[index].first },
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun ConfirmationSetting(
    title: String,
    dialogTitle: String,
    dialogMessage: String,
    onConfirm: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    OutlinedButton(onClick = { visible = true }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(title)
    }
    if (visible) {
        com.prof18.feedflow.desktop.ui.components.ConfirmationDialog(
            title = dialogTitle,
            message = dialogMessage,
            onConfirm = {
                if (visible) {
                    visible = false
                    latestOnConfirm()
                }
            },
            onDismiss = { visible = false },
        )
    }
}

private const val SHORT_CHOICE_LIMIT = 4
