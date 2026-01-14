package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.home.components.isMultiSelectModifierPressed
import com.prof18.feedflow.shared.ui.preview.feedSourcesForPreview
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.PreviewHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun DrawerFeedSourcesList(
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean) -> Unit,
    selectedFeedSourcesProvider: () -> List<FeedSource>,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onOpenWebsite: (String) -> Unit,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    dragState: FeedSourceDragState,
) {
    Column {
        drawerFeedSources.forEach { feedSourceWrapper ->
            val isMultiSelectPressed = isMultiSelectModifierPressed()
            val isMultiSelected = selectedFeedSourceIds.contains(feedSourceWrapper.feedSource.id)

            FeedSourceDrawerItem(
                label = {
                    Text(
                        text = feedSourceWrapper.feedSource.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = currentFeedFilter is FeedFilter.Source &&
                    currentFeedFilter.feedSource == feedSourceWrapper.feedSource,
                onClick = {
                    onFeedSourceClick(feedSourceWrapper.feedSource, isMultiSelectPressed)
                },
                icon = {
                    val imageUrl = feedSourceWrapper.feedSource.logoUrl
                    if (imageUrl != null) {
                        FeedSourceLogoImage(
                            size = 24.dp,
                            imageUrl = imageUrl,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                        )
                    }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                ),
                onEditFeedClick = onEditFeedClick,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onPinFeedClick = onPinFeedClick,
                onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                onOpenWebsite = onOpenWebsite,
                feedSource = feedSourceWrapper.feedSource,
                unreadCount = feedSourceWrapper.unreadCount,
                isMultiSelected = isMultiSelected,
                modifier = Modifier.Companion.feedSourceDragSource(
                    dragState = dragState,
                    feedSource = feedSourceWrapper.feedSource,
                    selectedFeedSources = selectedFeedSourcesProvider,
                    onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                ),
            )
        }
    }
}

@Composable
internal fun ColumnScope.FeedSourcesListWithCategorySelector(
    isCategoryExpanded: Boolean,
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean) -> Unit,
    selectedFeedSourcesProvider: () -> List<FeedSource>,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onOpenWebsite: (String) -> Unit,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    dragState: FeedSourceDragState,
) {
    ConditionalAnimatedVisibility(
        visible = isCategoryExpanded,
        enter = expandVertically(
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
        ),
        exit = shrinkVertically(),
    ) {
        DrawerFeedSourcesList(
            drawerFeedSources = drawerFeedSources,
            currentFeedFilter = currentFeedFilter,
            selectedFeedSourceIds = selectedFeedSourceIds,
            onFeedSourceClick = onFeedSourceClick,
            selectedFeedSourcesProvider = selectedFeedSourcesProvider,
            onEditFeedClick = onEditFeedClick,
            onDeleteFeedSourceClick = onDeleteFeedSourceClick,
            onPinFeedClick = onPinFeedClick,
            onChangeFeedCategoryClick = onChangeFeedCategoryClick,
            onOpenWebsite = onOpenWebsite,
            onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
            dragState = dragState,
        )
    }
}

@Preview
@Composable
private fun FeedSourcesListPreview() {
    PreviewHelper {
        DrawerFeedSourcesList(
            drawerFeedSources = feedSourcesForPreview.map {
                DrawerItem.DrawerFeedSource(
                    feedSource = it,
                    unreadCount = 10,
                )
            }.toImmutableList(),
            currentFeedFilter = FeedFilter.Timeline,
            selectedFeedSourceIds = persistentSetOf(),
            onFeedSourceClick = { _, _ -> },
            selectedFeedSourcesProvider = { emptyList() },
            onEditFeedClick = {},
            onDeleteFeedSourceClick = {},
            onPinFeedClick = {},
            onChangeFeedCategoryClick = {},
            onOpenWebsite = {},
            onMoveFeedSourcesToCategory = { _, _ -> },
            dragState = rememberFeedSourceDragState(rememberLazyListState()),
        )
    }
}
