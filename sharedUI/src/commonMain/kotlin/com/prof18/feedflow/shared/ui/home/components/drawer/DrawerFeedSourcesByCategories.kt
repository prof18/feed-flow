package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.shared.ui.components.EditCategoryNameDialog
import com.prof18.feedflow.shared.ui.feedsourcelist.singleAndLongClickModifier
import com.prof18.feedflow.shared.ui.home.components.CategoryContextMenu
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewHelper
import com.prof18.feedflow.shared.ui.utils.conditionalAnimateFloatAsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.collections.iterator

@Composable
internal fun DrawerFeedSourcesByCategories(
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean) -> Unit,
    selectedFeedSourcesProvider: () -> List<FeedSource>,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onDeleteCategoryClick: (CategoryId) -> Unit,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    dragState: FeedSourceDragState,
) {
    Column {
        Column {
            DrawerDivider()

            Text(
                modifier = Modifier
                    .padding(start = Spacing.regular)
                    .padding(bottom = Spacing.regular),
                text = LocalFeedFlowStrings.current.drawerTitleFeedSources,
                style = MaterialTheme.typography.labelLarge,
            )

            DrawerFeedSourcesList(
                drawerFeedSources = navDrawerState.feedSourcesWithoutCategory
                    .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
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

            for ((categoryWrapper, drawerFeedSources) in navDrawerState.feedSourcesByCategory) {
                var isCategoryExpanded by rememberSaveable {
                    mutableStateOf(false)
                }

                DrawerFeedSourceByCategoryItem(
                    feedSourceCategoryWrapper = categoryWrapper,
                    drawerFeedSources = drawerFeedSources
                        .filterIsInstance<DrawerItem.DrawerFeedSource>().toImmutableList(),
                    currentFeedFilter = currentFeedFilter,
                    isCategoryExpanded = isCategoryExpanded,
                    onCategoryExpand = {
                        isCategoryExpanded = !isCategoryExpanded
                    },
                    onFeedFilterSelected = onFeedFilterSelected,
                    selectedFeedSourceIds = selectedFeedSourceIds,
                    onFeedSourceClick = onFeedSourceClick,
                    selectedFeedSourcesProvider = selectedFeedSourcesProvider,
                    onEditFeedClick = onEditFeedClick,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                    onPinFeedClick = onPinFeedClick,
                    onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                    onOpenWebsite = onOpenWebsite,
                    onEditCategoryClick = onEditCategoryClick,
                    onDeleteCategoryClick = onDeleteCategoryClick,
                    onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                    dragState = dragState,
                )
            }
        }
    }
}

@Composable
internal fun DrawerFeedSourceByCategoryItem(
    feedSourceCategoryWrapper: DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper,
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    isCategoryExpanded: Boolean,
    onCategoryExpand: () -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean) -> Unit,
    selectedFeedSourcesProvider: () -> List<FeedSource>,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: ((FeedSource) -> Unit),
    onOpenWebsite: (String) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    dragState: FeedSourceDragState,
) {
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }

    val category = feedSourceCategoryWrapper.feedSourceCategory
    val unreadCount = drawerFeedSources.sumOf { it.unreadCount }
    val isDropTargetActive = dragState.isDragOver(category)

    FeedSourceDropTargetCleanup(
        dragState = dragState,
        category = category,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        @Suppress("MagicNumber")
        val degrees by conditionalAnimateFloatAsState(
            targetValue = if (isCategoryExpanded) -90f else 90f,
            animationSpec = spring(),
            label = "Category arrow animation",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val headerText = category?.title ?: LocalFeedFlowStrings.current.noCategory
            val isSelected = if (category != null) {
                currentFeedFilter is FeedFilter.Category && currentFeedFilter.feedCategory == category
            } else {
                currentFeedFilter is FeedFilter.Uncategorized
            }
            val navItemColors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
            )
            val dropTargetModifier = Modifier.Companion.dropTargetModifier(
                dragState = dragState,
                category = category,
                isDropTargetActive = isDropTargetActive,
                highlightColor = MaterialTheme.colorScheme.primary,
            )
            val onClick = {
                if (category != null) {
                    onFeedFilterSelected(FeedFilter.Category(feedCategory = category))
                } else {
                    onFeedFilterSelected(FeedFilter.Uncategorized)
                }
            }

            Surface(
                selected = isSelected,
                onClick = onClick,
                shape = CircleShape,
                color = navItemColors.containerColor(isSelected).value,
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.Tab }
                    .heightIn(min = 56.dp)
                    .hoverable(remember { MutableInteractionSource() })
                    .then(dropTargetModifier),
            ) {
                val contentModifier = if (category != null) {
                    Modifier.singleAndLongClickModifier(
                        onClick = onClick,
                        onLongClick = { showMenu = true },
                    )
                } else {
                    Modifier
                }

                Row(
                    modifier = contentModifier.padding(start = Spacing.regular, end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val labelColor = navItemColors.textColor(isSelected).value
                    CompositionLocalProvider(LocalContentColor provides labelColor) {
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (unreadCount > 0) {
                        Text(
                            modifier = Modifier.padding(start = Spacing.small),
                            text = unreadCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = navItemColors.textColor(isSelected).value,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                    ) { onCategoryExpand() }
                    .padding(12.dp)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(degrees),
                )
            }
        }

        FeedSourcesListWithCategorySelector(
            isCategoryExpanded = isCategoryExpanded,
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

    if (category != null) {
        CategoryContextMenu(
            showMenu = showMenu,
            hideMenu = { showMenu = false },
            categoryId = CategoryId(category.id),
            onEditCategoryClick = {
                showMenu = false
                showEditDialog = true
            },
            onDeleteCategoryClick = onDeleteCategoryClick,
        )

        EditCategoryNameDialog(
            showDialog = showEditDialog,
            categoryId = CategoryId(category.id),
            initialCategoryName = category.title,
            onDismiss = { showEditDialog = false },
            onEditCategory = onEditCategoryClick,
        )
    }
}

@Preview
@Composable
private fun DrawerFeedSourcesByCategoriesPreview() {
    PreviewHelper {
        DrawerFeedSourcesByCategories(
            navDrawerState = NavDrawerState(
                timeline = persistentListOf(),
                read = persistentListOf(),
                bookmarks = persistentListOf(),
                pinnedFeedSources = persistentListOf(),
                feedSourcesWithoutCategory = persistentListOf(),
                feedSourcesByCategory = persistentMapOf(),
                categories = persistentListOf(),
            ),
            currentFeedFilter = FeedFilter.Timeline,
            onFeedFilterSelected = {},
            selectedFeedSourceIds = persistentSetOf(),
            onFeedSourceClick = { _, _ -> },
            selectedFeedSourcesProvider = { emptyList() },
            onEditFeedClick = {},
            onDeleteFeedSourceClick = {},
            onPinFeedClick = {},
            onOpenWebsite = {},
            onEditCategoryClick = { _, _ -> },
            onChangeFeedCategoryClick = {},
            onDeleteCategoryClick = {},
            onMoveFeedSourcesToCategory = { _, _ -> },
            dragState = rememberFeedSourceDragState(rememberLazyListState()),
        )
    }
}
