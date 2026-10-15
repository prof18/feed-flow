package com.prof18.feedflow.desktop.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.rememberScrollbarState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.desktop.ui.components.LocalWindowTopInset
import com.prof18.feedflow.desktop.ui.components.rememberDesktopSidebarItemStyle
import org.jetbrains.jewel.foundation.lazy.SingleSelectionLazyColumn
import org.jetbrains.jewel.foundation.lazy.items
import org.jetbrains.jewel.foundation.lazy.rememberSingleSelectionLazyListState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.SimpleListItemStyle

@Composable
internal fun SettingsContent(
    initialCategory: DesktopSettingsCategory,
    modifier: Modifier = Modifier,
    sectionKey: String? = null,
    onCategorySelected: (DesktopSettingsCategory) -> Unit = {},
    content: @Composable (DesktopSettingsCategory) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val categories = listOf(
        DesktopSettingsCategory.GENERAL to strings.settingsAppTitle,
        DesktopSettingsCategory.ACCOUNTS to strings.settingsAccounts,
        DesktopSettingsCategory.FEED_LIST to strings.settingsFeedListTitle,
        DesktopSettingsCategory.READING to strings.settingsReadingBehavior,
        DesktopSettingsCategory.FILTERS to strings.settingsFilters,
        DesktopSettingsCategory.SYNC_STORAGE to strings.settingsSyncAndStorage,
        DesktopSettingsCategory.ABOUT to strings.settingsAboutAndSupport,
    )
    val categoryListState = rememberSingleSelectionLazyListState(initialSelectedKey = initialCategory)
    val categoryStyle = rememberDesktopSidebarItemStyle()
    val saveableStateHolder = rememberSaveableStateHolder()
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    val categorySelected by rememberUpdatedState(onCategorySelected)
    val selectCategory: (DesktopSettingsCategory) -> Unit = { category ->
        categoryListState.selectedKeys = setOf(category)
        selectedCategory = category
        categorySelected(category)
    }

    LaunchedEffect(initialCategory) {
        categoryListState.selectedKeys = setOf(initialCategory)
        selectedCategory = initialCategory
    }
    LaunchedEffect(categoryListState.selectedKeys) {
        (categoryListState.selectedKeys.firstOrNull() as? DesktopSettingsCategory)?.let {
            if (selectedCategory != it) selectCategory(it)
        }
    }

    val separator = settingsSeparatorColor()
    val topInset = LocalWindowTopInset.current
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
                    .drawTrailingSeparator(separator)
                    .padding(top = topInset)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            ) {
                items(items = categories, key = { it.first }) { (category, label) ->
                    SettingsCategoryRow(
                        label = label,
                        icon = category.icon(),
                        state = ListItemState(isSelected = isSelected, isActive = isActive),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                            .clickable { selectCategory(category) },
                        style = categoryStyle,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 8.dp + topInset, end = 8.dp, bottom = 8.dp),
            ) {
                saveableStateHolder.SaveableStateProvider("${selectedCategory.name}:${sectionKey.orEmpty()}") {
                    content(selectedCategory)
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    label: String,
    icon: ImageVector,
    state: ListItemState,
    style: SimpleListItemStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                selected = state.isSelected
                isTraversalGroup = false
            }
            .heightIn(min = 36.dp)
            .background(
                color = style.colors.backgroundFor(state).value,
                shape = RoundedCornerShape(DesktopDesignTokens.buttonCornerSize),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val contentColor = style.colors.contentFor(state).value.takeOrElse { JewelTheme.globalColors.text.normal }
        Icon(
            icon,
            contentDescription = null,
            tint = if (state.isSelected) contentColor else JewelTheme.globalColors.text.info,
            modifier = Modifier.size(CATEGORY_ICON_SIZE),
        )
        Text(
            text = label,
            color = contentColor,
            fontWeight = if (state.isSelected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

private fun DesktopSettingsCategory.icon(): ImageVector = when (this) {
    DesktopSettingsCategory.GENERAL -> Icons.Outlined.Tune
    DesktopSettingsCategory.ACCOUNTS -> Icons.Outlined.ManageAccounts
    DesktopSettingsCategory.FEED_LIST -> Icons.AutoMirrored.Outlined.ViewList
    DesktopSettingsCategory.READING -> Icons.Outlined.AutoStories
    DesktopSettingsCategory.FILTERS -> Icons.Outlined.FilterAlt
    DesktopSettingsCategory.SYNC_STORAGE -> Icons.Outlined.Sync
    DesktopSettingsCategory.ABOUT -> Icons.Outlined.Info
}

internal const val SETTINGS_CATEGORIES_TAG = "settings-categories"
private val NAVIGATION_WIDTH = 216.dp
private val NAVIGATION_MIN_WIDTH = 184.dp
private val NAVIGATION_COMPACT_BREAKPOINT = 800.dp
private val CATEGORY_ICON_SIZE = 18.dp
private val SETTINGS_CONTENT_MAX_WIDTH = 680.dp
private val SETTINGS_SIDE_PREVIEW_MIN_WIDTH = 720.dp
private val SETTINGS_SIDE_PREVIEW_WIDTH = 336.dp
private val SETTINGS_CARD_SHAPE = RoundedCornerShape(10.dp)

/** Page-level heading for a settings category. */
@Composable
internal fun SettingsGroup(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
        style = settingsPageTitleStyle(),
    )
}

@Composable
private fun settingsPageTitleStyle() = JewelTheme.defaultTextStyle.copy(
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
)

/**
 * Scrollable settings page with a page title and a width-constrained column of [SettingsSection]s.
 */
@Composable
internal fun SettingsPage(
    title: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    preview: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (preview == null) {
        SettingsScrollPane(modifier = modifier.fillMaxSize(), scrollState = scrollState) {
            SettingsPageColumn(top = 8.dp) {
                SettingsPageTitle(title)
                content()
            }
        }
        return
    }
    // A live preview of what the options change: beside them when there is room, pinned above them otherwise.
    val previewTitle = LocalFeedFlowStrings.current.desktopSettingsPreviewTitle
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= SETTINGS_SIDE_PREVIEW_MIN_WIDTH) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.padding(start = 16.dp, top = 8.dp)) { SettingsPageTitle(title) }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Row(Modifier.fillMaxSize().padding(end = 12.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(scrollState)) {
                            SettingsPageColumn(top = 24.dp, content = content)
                        }
                        Box(Modifier.width(SETTINGS_SIDE_PREVIEW_WIDTH).padding(end = 16.dp, top = 24.dp)) {
                            SettingsSection(title = previewTitle) { preview() }
                        }
                    }
                    // One scrollbar at the window edge, so it never splits the controls from their preview.
                    if (scrollState.maxValue > 0) {
                        FeedFlowVerticalScrollbar(rememberScrollbarState(scrollState))
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.padding(end = 12.dp)) {
                    SettingsPageColumn(top = 8.dp, bottom = 16.dp) {
                        SettingsPageTitle(title)
                        SettingsSection(title = previewTitle) { preview() }
                    }
                }
                if (scrollState.value > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(settingsSeparatorColor()))
                }
                SettingsScrollPane(modifier = Modifier.weight(1f).fillMaxWidth(), scrollState = scrollState) {
                    SettingsPageColumn(top = 8.dp, content = content)
                }
            }
        }
    }
}

@Composable
private fun SettingsPageColumn(
    top: Dp,
    bottom: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = SETTINGS_CONTENT_MAX_WIDTH)
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = top, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}

@Composable
private fun SettingsPageTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
        style = settingsPageTitleStyle(),
    )
}

@Composable
internal fun SettingsScrollPane(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
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

/**
 * A titled, rounded card grouping related settings rows. Rows are separated by hairline dividers.
 */
@Composable
internal fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                fontWeight = FontWeight.SemiBold,
                color = JewelTheme.globalColors.text.normal,
            )
        }
        val border = settingsSeparatorColor()
        CompositionLocalProvider(LocalInSettingsSection provides true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SETTINGS_CARD_SHAPE)
                    .background(settingsCardColor())
                    .border(1.dp, border, SETTINGS_CARD_SHAPE),
                content = content,
            )
        }
        if (footer != null) {
            Text(
                text = footer,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                style = DesktopDesignTokens.metadataText,
                color = JewelTheme.globalColors.text.info,
            )
        }
    }
}

/**
 * Base row used inside a [SettingsSection]: label and optional description on the leading side,
 * the [trailing] control on the opposite side. A hairline separator is drawn above every row; the
 * first one overlaps the card border, so only separators between rows are visible.
 */
@Composable
internal fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .settingsRowSeparator()
            .heightIn(min = SETTINGS_ROW_MIN_HEIGHT)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) {
            Box(Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA)) { leading() }
        }
        Column(
            modifier = Modifier.weight(1f).alpha(if (enabled) 1f else DISABLED_ALPHA),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = titleColor)
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = DesktopDesignTokens.metadataText,
                    color = JewelTheme.globalColors.text.info,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** A setting row toggled by a trailing switch. The whole row is the toggle target. */
@Composable
internal fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    SettingsRow(
        title = title,
        supportingText = supportingText,
        enabled = enabled,
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange,
            ),
        trailing = { SettingsSwitchTrack(checked = checked, enabled = enabled, focused = focused) },
    )
}

@Composable
private fun SettingsSwitchTrack(checked: Boolean, enabled: Boolean, focused: Boolean) {
    val accent = JewelTheme.globalColors.outlines.focused
    val offTrack = JewelTheme.globalColors.text.normal.copy(alpha = if (JewelTheme.isDark) 0.28f else 0.2f)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) SWITCH_WIDTH - SWITCH_THUMB - SWITCH_INSET * 2 else 0.dp,
        animationSpec = tween(durationMillis = 120),
    )
    val direction = LocalLayoutDirection.current
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(
                if (focused) {
                    Modifier.border(2.dp, accent.copy(alpha = 0.5f), CircleShape).padding(2.dp)
                } else {
                    Modifier.padding(2.dp)
                },
            )
            .width(SWITCH_WIDTH)
            .height(SWITCH_HEIGHT)
            .background(if (checked) accent else offTrack, CircleShape)
            .padding(SWITCH_INSET),
        contentAlignment = if (direction == LayoutDirection.Ltr) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .offset(x = if (direction == LayoutDirection.Ltr) thumbOffset else -thumbOffset)
                .size(SWITCH_THUMB)
                .background(Color.White, CircleShape),
        )
    }
}

/** A row that performs an action or navigates when clicked. */
@Composable
internal fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    showChevron: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val highlight = JewelTheme.globalColors.text.normal.copy(alpha = 0.05f)
    SettingsRow(
        title = title,
        supportingText = supportingText,
        enabled = enabled,
        titleColor = if (destructive) JewelTheme.globalColors.text.error else Color.Unspecified,
        leading = leading,
        modifier = modifier
            .background(if (enabled && (hovered || focused)) highlight else Color.Transparent)
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        trailing = when {
            trailing != null -> trailing
            showChevron -> {
                {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = JewelTheme.globalColors.text.info,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            else -> null
        },
    )
}

/** Free-form content placed inside a [SettingsSection] with row separators and padding. */
@Composable
internal fun SettingsBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .settingsRowSeparator()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
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

@Composable
internal fun ConfirmationSetting(
    title: String,
    dialogTitle: String,
    dialogMessage: String,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    val latestEnabled by rememberUpdatedState(enabled)
    SettingsActionRow(
        title = title,
        supportingText = supportingText,
        onClick = { if (latestEnabled) visible = true },
        enabled = enabled,
        destructive = true,
    )
    if (visible) {
        com.prof18.feedflow.desktop.ui.components.ConfirmationDialog(
            title = dialogTitle,
            message = dialogMessage,
            onConfirm = {
                if (visible) {
                    visible = false
                    if (latestEnabled) latestOnConfirm()
                }
            },
            onDismiss = { visible = false },
        )
    }
}

@Composable
private fun settingsCardColor(): Color = JewelTheme.globalColors.text.normal
    .copy(alpha = if (JewelTheme.isDark) 0.045f else 0.028f)
    .compositeOver(JewelTheme.globalColors.panelBackground)

@Composable
private fun settingsSeparatorColor(): Color = JewelTheme.globalColors.text.normal
    .copy(alpha = if (JewelTheme.isDark) 0.12f else 0.09f)
    .compositeOver(JewelTheme.globalColors.panelBackground)

// Lets rows draw separators only when they sit inside a SettingsSection card.
@Suppress("CompositionLocalAllowlist")
internal val LocalInSettingsSection = staticCompositionLocalOf { false }

@Composable
internal fun Modifier.settingsRowSeparator(): Modifier =
    if (LocalInSettingsSection.current) drawRowSeparator(settingsSeparatorColor()) else this

private fun Modifier.drawRowSeparator(color: Color): Modifier = drawBehind {
    val inset = ROW_SEPARATOR_INSET.toPx()
    val (start, end) = if (layoutDirection == LayoutDirection.Ltr) inset to size.width else 0f to size.width - inset
    drawLine(color, Offset(start, 0f), Offset(end, 0f), strokeWidth = 1.dp.toPx())
}

private fun Modifier.drawTrailingSeparator(color: Color): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val x = if (layoutDirection == LayoutDirection.Ltr) size.width - stroke / 2 else stroke / 2
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
}

private val SETTINGS_ROW_MIN_HEIGHT = 44.dp
private val ROW_SEPARATOR_INSET: Dp = 14.dp
private val SWITCH_WIDTH = 34.dp
private val SWITCH_HEIGHT = 20.dp
private val SWITCH_THUMB = 16.dp
private val SWITCH_INSET = 2.dp
private const val DISABLED_ALPHA = 0.45f
