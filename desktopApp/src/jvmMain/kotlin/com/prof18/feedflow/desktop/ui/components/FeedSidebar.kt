@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.home.projectDesktopFeedTree
import com.prof18.feedflow.desktop.home.rememberDesktopFeedTreeState
import com.prof18.feedflow.desktop.main.DesktopDialogWindowDestination
import com.prof18.feedflow.desktop.main.LocalDesktopDialogWindowNavigator
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun FeedSidebar(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
    onFeedSuggestionsClick: () -> Unit = {},
    onImportExportClick: () -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    val navigator = LocalDesktopDialogWindowNavigator.current
    val navDrawerState = displayState.navDrawerState
    val rows = remember(navDrawerState, strings, displayState.isUnreadCountHidden) {
        buildSidebarRows(
            navDrawerState,
            displayState.isUnreadCountHidden,
            strings.drawerTitleTimeline,
            strings.drawerTitleRead,
            strings.drawerTitleBookmarks,
            strings.noCategory,
        )
    }
    val treeState = rememberDesktopFeedTreeState()
    LaunchedEffect(navDrawerState) { treeState.reconcile(projectDesktopFeedTree(navDrawerState)) }
    val collapsedSections = treeState.collapsedSectionIds
    val visibleRows = rows.filter { it.parentSection !in collapsedSections }
    val filterByKey = rows.associate { it.entry.key to it.filter }
    val navEntries = visibleRows.map { row ->
        val label = row.filter.sidebarLabel(
            strings.drawerTitleTimeline,
            strings.drawerTitleRead,
            strings.drawerTitleBookmarks,
            strings.noCategory,
        )
        row.entry.copy(label = label)
    }.toImmutableList()

    Column(
        modifier.fillMaxSize()
            .background(JewelTheme.globalColors.toolwindowBackground)
            .padding(12.dp),
    ) {
        SidebarList(
            entries = navEntries,
            activeKey = displayState.currentFeedFilter.sidebarKey(),
            onActivate = { key -> filterByKey[key]?.let(onFeedFilterSelected) },
            modifier = Modifier.weight(1f),
            onSelectionChanged = { navigator.feedSourceSelection = it.toPersistentSet() },
            onFocusedKeyChanged = { key -> treeState.focusedKey = key?.feedTreeFocusKey() },
            onExpansionChange = { key, expanded ->
                val section = rows.firstOrNull { it.entry.key == key }?.section
                if (section != null) {
                    treeState.setExpanded(section, expanded)
                }
            },
            leadingIcon = { key ->
                when (key) {
                    SidebarKey.Timeline -> AllIconsKeys.Actions.ListFiles
                    SidebarKey.Read -> AllIconsKeys.Actions.Checked
                    SidebarKey.Bookmarks -> AllIconsKeys.Toolwindows.ToolWindowBookmarks
                    else -> null
                }
            },
            sectionHeader = {
                FeedSourcesSectionHeader(
                    title = strings.drawerTitleFeedSources,
                    addFeedLabel = strings.addFeed,
                    onManage = { navigator.open(DesktopDialogWindowDestination.FeedSources) },
                    onAddFeed = feedManagementActions.onAddFeedClick,
                )
            },
            trailingContent = { key ->
                val count = visibleRows.firstOrNull { it.entry.key == key }?.unreadCount ?: 0L
                if (!displayState.isUnreadCountHidden && count > 0) {
                    Text(
                        count.toString(),
                        color = JewelTheme.globalColors.text.info,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }

                val section = visibleRows.firstOrNull { it.entry.key == key }?.section
                if (section != null) {
                    val collapsed = section in collapsedSections
                    val title = filterByKey[key]?.let {
                        if (it is FeedFilter.Category) it.feedCategory.title else strings.noCategory
                    }.orEmpty()
                    val description = if (collapsed) {
                        strings.expandCategoryButtonContentDescription(title)
                    } else {
                        strings.collapseCategoryButtonContentDescription(title)
                    }
                    Tooltip(tooltip = { Text(description) }) {
                        IconButton(focusable = false, onClick = {
                            treeState.setExpanded(section, collapsed)
                        }) {
                            Icon(
                                key = if (collapsed) {
                                    AllIconsKeys.General.ArrowRight
                                } else {
                                    AllIconsKeys.General.ArrowDown
                                },
                                contentDescription = description,
                            )
                        }
                    }
                }
            },
        )
        Column {
            Box(
                Modifier.fillMaxWidth()
                    .height(1.dp)
                    .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.5f)),
            )
            SidebarFooterAction(
                label = strings.drawerTitleFeedSources,
                icon = AllIconsKeys.Nodes.Folder,
                onClick = { navigator.open(DesktopDialogWindowDestination.FeedSources) },
            )
            SidebarFooterAction(
                label = strings.feedSuggestionsTitle,
                icon = AllIconsKeys.Actions.IntentionBulb,
                onClick = onFeedSuggestionsClick,
            )
            SidebarFooterAction(
                label = strings.importExportLabel,
                icon = AllIconsKeys.Actions.SwapPanels,
                onClick = onImportExportClick,
            )
        }
    }
}

@Composable
private fun FeedSourcesSectionHeader(
    title: String,
    addFeedLabel: String,
    onManage: () -> Unit,
    onAddFeed: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f)
                .clickable(role = Role.Button, onClick = onManage),
        )
        Tooltip(tooltip = { Text(addFeedLabel) }) {
            IconButton(onClick = onAddFeed) {
                Icon(AllIconsKeys.General.Add, contentDescription = addFeedLabel)
            }
        }
    }
}

@Composable
private fun SidebarFooterAction(label: String, icon: IconKey, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 36.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, modifier = Modifier.weight(1f).padding(start = 10.dp))
    }
}

internal fun FeedFilter.sidebarKey(): SidebarKey = when (this) {
    FeedFilter.Timeline -> SidebarKey.Timeline
    FeedFilter.Read -> SidebarKey.Read
    FeedFilter.Bookmarks -> SidebarKey.Bookmarks
    FeedFilter.Uncategorized -> SidebarKey.Uncategorized
    is FeedFilter.Source -> SidebarKey.Feed(feedSource.id)
    is FeedFilter.Category -> SidebarKey.Category(feedCategory.id)
}

private fun FeedFilter.sidebarLabel(
    timelineTitle: String,
    readTitle: String,
    bookmarksTitle: String,
    uncategorizedTitle: String,
): String = when (this) {
    FeedFilter.Timeline -> timelineTitle
    FeedFilter.Read -> readTitle
    FeedFilter.Bookmarks -> bookmarksTitle
    FeedFilter.Uncategorized -> uncategorizedTitle
    is FeedFilter.Source -> feedSource.title
    is FeedFilter.Category -> feedCategory.title
}

private fun SidebarKey.feedTreeFocusKey(): String = when (this) {
    SidebarKey.Timeline -> "timeline"
    SidebarKey.Read -> "read"
    SidebarKey.Bookmarks -> "bookmarks"
    SidebarKey.Uncategorized -> "uncategorized"
    is SidebarKey.Category -> "category:$id"
    is SidebarKey.Feed -> "feed:$id"
}
