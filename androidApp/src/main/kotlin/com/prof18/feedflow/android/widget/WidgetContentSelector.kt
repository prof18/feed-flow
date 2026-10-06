package com.prof18.feedflow.android.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.exposeTestTagsAsResourceIds

@Composable
fun WidgetContentSelector(
    state: WidgetSettingsState,
    onSelected: (WidgetContentFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val selected = state.contentFilter.resolveAgainst(state)
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.padding(Spacing.regular)) {
        Text(
            text = strings.widgetContentSectionTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
        Surface(
            onClick = { showPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("widget_content_selector"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(Spacing.regular),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
            ) {
                if (selected is WidgetContentFilter.Source) {
                    WidgetSourceLogo(state.feedSources.firstOrNull { it.id == selected.feedSourceId }?.logoUrl)
                } else {
                    Icon(
                        selected.widgetContentIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.widgetContentTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when (selected) {
                            WidgetContentFilter.Timeline -> strings.widgetContentTimelineDescription
                            WidgetContentFilter.Bookmarks -> strings.widgetContentBookmarksDescription
                            is WidgetContentFilter.Category -> strings.widgetContentCategory
                            is WidgetContentFilter.Source -> strings.widgetContentFeedSource
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = strings.widgetContentChoose)
            }
        }
    }

    if (showPicker) {
        WidgetContentPicker(
            state = state,
            selected = selected,
            onDismiss = { showPicker = false },
            onSelected = { filter ->
                onSelected(filter)
                showPicker = false
            },
        )
    }
}

@Composable
private fun WidgetContentPicker(
    state: WidgetSettingsState,
    selected: WidgetContentFilter,
    onDismiss: () -> Unit,
    onSelected: (WidgetContentFilter) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    var search by rememberSaveable { mutableStateOf("") }
    val query = search.trim()
    val categories = remember(state.categories, query) {
        state.categories.filter { it.title.contains(query, ignoreCase = true) }.sortedBy { it.title.lowercase() }
    }
    val sources = remember(state.feedSources, query) {
        state.feedSources.filter {
            it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }.sortedBy { it.title.lowercase() }
    }
    val showTimeline = strings.widgetLatestItems.contains(query, ignoreCase = true)
    val showBookmarks = strings.drawerTitleBookmarks.contains(query, ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(fraction = 0.9f)
                .exposeTestTagsAsResourceIds(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.regular),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.widgetContentChoose,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.closeButtonContentDescription)
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.regular)
                    .testTag("widget_content_search"),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (showTimeline) {
                    item(key = "timeline") {
                        WidgetContentChoice(
                            title = strings.widgetLatestItems,
                            subtitle = strings.widgetContentTimelineDescription,
                            filter = WidgetContentFilter.Timeline,
                            selected = selected,
                            leadingContent = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onSelected = onSelected,
                        )
                    }
                }
                if (showBookmarks) {
                    item(key = "bookmarks") {
                        WidgetContentChoice(
                            title = strings.drawerTitleBookmarks,
                            subtitle = strings.widgetContentBookmarksDescription,
                            filter = WidgetContentFilter.Bookmarks,
                            selected = selected,
                            leadingContent = {
                                Icon(
                                    Icons.Default.Bookmarks,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onSelected = onSelected,
                        )
                    }
                }
                if (categories.isNotEmpty() || query.isEmpty()) {
                    item { WidgetContentSectionTitle(strings.drawerTitleCategories) }
                    if (state.categories.isEmpty()) {
                        item { WidgetContentEmptyMessage(strings.widgetContentNoCategories) }
                    }
                    items(categories, key = { "category:${it.id}" }) { category ->
                        WidgetContentChoice(
                            title = category.title,
                            subtitle = strings.widgetContentCategory,
                            filter = WidgetContentFilter.Category(category.id),
                            selected = selected,
                            leadingContent = {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onSelected = onSelected,
                        )
                    }
                }
                if (sources.isNotEmpty() || query.isEmpty()) {
                    item { WidgetContentSectionTitle(strings.drawerTitleFeedSources) }
                    if (state.feedSources.isEmpty()) {
                        item { WidgetContentEmptyMessage(strings.widgetCheckFeedSources) }
                    }
                    items(sources, key = { "source:${it.id}" }) { source ->
                        WidgetContentChoice(
                            title = source.title,
                            subtitle = source.url,
                            filter = WidgetContentFilter.Source(source.id),
                            selected = selected,
                            leadingContent = { WidgetSourceLogo(source.logoUrl) },
                            onSelected = onSelected,
                        )
                    }
                }
                if (!showTimeline && !showBookmarks && categories.isEmpty() && sources.isEmpty()) {
                    item { WidgetContentEmptyMessage(strings.searchNoData(query)) }
                }
            }
        }
    }
}

@Composable
private fun WidgetContentChoice(
    title: String,
    subtitle: String,
    filter: WidgetContentFilter,
    selected: WidgetContentFilter,
    leadingContent: @Composable () -> Unit,
    onSelected: (WidgetContentFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = filter == selected, role = Role.RadioButton, onClick = { onSelected(filter) })
            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        leadingContent()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RadioButton(selected = filter == selected, onClick = null)
    }
}

@Composable
private fun WidgetSourceLogo(logoUrl: String?, modifier: Modifier = Modifier) {
    if (logoUrl.isNullOrBlank()) {
        Icon(
            Icons.Default.RssFeed,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        FeedSourceLogoImage(
            imageUrl = logoUrl,
            size = 24.dp,
            modifier = modifier,
            fallbackIcon = Icons.Default.RssFeed,
        )
    }
}

@Composable
private fun WidgetContentSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun WidgetContentEmptyMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier = modifier.padding(Spacing.regular),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun WidgetContentFilter.widgetContentIcon(): ImageVector = when (this) {
    WidgetContentFilter.Timeline -> Icons.Default.Schedule
    WidgetContentFilter.Bookmarks -> Icons.Default.Bookmarks
    is WidgetContentFilter.Category -> Icons.Default.Folder
    is WidgetContentFilter.Source -> Icons.Default.RssFeed
}

@Composable
internal fun WidgetSettingsState.widgetContentTitle(): String {
    val strings = LocalFeedFlowStrings.current
    return when (val filter = contentFilter.resolveAgainst(this)) {
        WidgetContentFilter.Timeline -> strings.widgetLatestItems
        WidgetContentFilter.Bookmarks -> strings.drawerTitleBookmarks
        is WidgetContentFilter.Category -> categories.first { it.id == filter.categoryId }.title
        is WidgetContentFilter.Source -> feedSources.first { it.id == filter.feedSourceId }.title
    }
}

private fun WidgetContentFilter.resolveAgainst(state: WidgetSettingsState): WidgetContentFilter = when (this) {
    is WidgetContentFilter.Category -> takeIf { state.categories.any { it.id == categoryId } }
    is WidgetContentFilter.Source -> takeIf { state.feedSources.any { it.id == feedSourceId } }
    else -> this
} ?: WidgetContentFilter.Timeline
