package com.prof18.feedflow.android.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.prof18.feedflow.android.widget.FeedFlowWidget
import com.prof18.feedflow.android.widget.widgetContentIcon
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

@Composable
fun WidgetSettingsScreen(
    navigateBack: () -> Unit,
    navigateToWidgetInstanceSettings: (Int) -> Unit,
) {
    val context = LocalContext.current
    val widgetSettingsRepository = koinInject<WidgetSettingsRepository>()
    val widgetRepository = koinInject<FeedWidgetRepository>()
    val settingsRepository = koinInject<SettingsRepository>()
    val syncPeriod by settingsRepository.syncPeriodFlow.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var widgets by remember {
        mutableStateOf<ImmutableList<WidgetListItem>>(persistentListOf())
    }

    LaunchedEffect(context, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val manager = GlanceAppWidgetManager(context)
            val categories = widgetRepository.getFeedSourceCategories()
            val feedSources = widgetRepository.getFeedSources()
            widgets = manager.getGlanceIds(FeedFlowWidget::class.java)
                .map(manager::getAppWidgetId)
                .sorted()
                .map { appWidgetId ->
                    WidgetListItem(
                        appWidgetId = appWidgetId,
                        contentFilter = widgetSettingsRepository.getWidgetConfiguration(appWidgetId).contentFilter,
                    )
                }
                .map { item ->
                    item.copy(
                        subtitle = when (val filter = item.contentFilter) {
                            WidgetContentFilter.Timeline,
                            WidgetContentFilter.Bookmarks,
                            -> null

                            is WidgetContentFilter.Category ->
                                categories.firstOrNull { it.id == filter.categoryId }?.title

                            is WidgetContentFilter.Source ->
                                feedSources.firstOrNull { it.id == filter.feedSourceId }?.title
                        },
                    )
                }
                .toImmutableList()
        }
    }

    WidgetSettingsScreenContent(
        widgets = widgets,
        syncPeriod = syncPeriod,
        navigateBack = navigateBack,
        navigateToWidgetInstanceSettings = navigateToWidgetInstanceSettings,
    )
}

@Composable
private fun WidgetSettingsScreenContent(
    widgets: ImmutableList<WidgetListItem>,
    syncPeriod: SyncPeriod,
    navigateBack: () -> Unit,
    navigateToWidgetInstanceSettings: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(strings.widgetConfigurationTitle) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
        ) {
            item {
                Text(
                    text = strings.widgetConfigurationDescription,
                    modifier = Modifier.padding(Spacing.regular),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (syncPeriod == SyncPeriod.NEVER) {
                item {
                    Text(
                        text = strings.widgetBackgroundSyncDisabledWarning,
                        modifier = Modifier.padding(horizontal = Spacing.regular),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (widgets.isEmpty()) {
                item {
                    Text(
                        text = strings.widgetListEmpty,
                        modifier = Modifier.padding(Spacing.regular),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(widgets, key = WidgetListItem::appWidgetId) { widget ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.xsmall),
                    ) {
                        WidgetListRow(
                            widget = widget,
                            widgetNumber = widgets.indexOf(widget) + 1,
                            onClick = { navigateToWidgetInstanceSettings(widget.appWidgetId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetListRow(
    widget: WidgetListItem,
    widgetNumber: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val subtitle = widget.subtitle ?: when (widget.contentFilter) {
        WidgetContentFilter.Timeline -> strings.widgetLatestItems
        WidgetContentFilter.Bookmarks -> strings.drawerTitleBookmarks
        is WidgetContentFilter.Category,
        is WidgetContentFilter.Source,
        -> strings.widgetLatestItems
    }

    val contentType = when (widget.contentFilter) {
        WidgetContentFilter.Timeline -> strings.widgetContentSectionTitle
        WidgetContentFilter.Bookmarks -> strings.widgetContentSectionTitle
        is WidgetContentFilter.Category -> strings.widgetContentCategory
        is WidgetContentFilter.Source -> strings.widgetContentFeedSource
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            widget.contentFilter.widgetContentIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xsmall)) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = strings.widgetListItemDescription(widgetNumber.toString(), contentType),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

private data class WidgetListItem(
    val appWidgetId: Int,
    val contentFilter: WidgetContentFilter,
    val subtitle: String? = null,
)
