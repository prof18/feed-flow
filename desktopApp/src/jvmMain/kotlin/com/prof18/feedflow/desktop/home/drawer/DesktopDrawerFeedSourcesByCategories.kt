package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.DesktopPopupMenu
import com.prof18.feedflow.desktop.ui.components.DesktopPopupMenuEntry
import com.prof18.feedflow.desktop.ui.conditionalAnimateFloatAsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopDrawerFeedSourcesByCategories(
    navDrawerState: NavDrawerState,
    onDeleteAllFeedsInCategoryClick: (List<FeedSource>) -> Unit,
    onAddFeedClick: () -> Unit,
    onFeedSuggestionsClick: () -> Unit = {},
    onImportFeedsClick: () -> Unit = {},
    onExpandAll: () -> Unit = {},
    onCollapseAll: () -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    var showOptions by remember { mutableStateOf(false) }
    var menuCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var showAddOptions by remember { mutableStateOf(false) }
    var addMenuCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val uncategorizedSources = remember(navDrawerState.feedSourcesWithoutCategory) {
        navDrawerState.feedSourcesWithoutCategory.filterIsInstance<DrawerItem.DrawerFeedSource>()
            .map { it.feedSource }
    }
    var showDeleteAllUncategorizedDialog by remember { mutableStateOf(false) }
    DrawerDivider()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SidebarSectionLabel(strings.drawerTitleFeedSources, modifier = Modifier.weight(1f))
        Box(Modifier.onGloballyPositioned { addMenuCoordinates = it }) {
            Tooltip(tooltip = { Text(strings.addFeed) }) {
                IconButton(onClick = { showAddOptions = true }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.addFeed,
                        tint = JewelTheme.globalColors.text.normal,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Box(Modifier.onGloballyPositioned { menuCoordinates = it }) {
            Tooltip(tooltip = { Text(strings.moreOptionsButtonContentDescription) }) {
                IconButton(onClick = { showOptions = true }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = strings.moreOptionsButtonContentDescription,
                        tint = JewelTheme.globalColors.text.normal,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
    val menuEntries = persistentListOf(
        DesktopPopupMenuEntry.Action(strings.desktopExpandAllFeeds, onClick = {
            showOptions = false
            onExpandAll()
        }),
        DesktopPopupMenuEntry.Action(strings.desktopCollapseAllFeeds, onClick = {
            showOptions = false
            onCollapseAll()
        }),
    ).let { entries ->
        if (uncategorizedSources.isNotEmpty() && navDrawerState.feedSourcesByCategory.isEmpty()) {
            entries.add(
                DesktopPopupMenuEntry.Action(
                    strings.deleteAllFeedsInCategory,
                    onClick = {
                        showOptions = false
                        showDeleteAllUncategorizedDialog = true
                    },
                ),
            )
        } else {
            entries
        }
    }
    DesktopPopupMenu(
        showMenu = showAddOptions,
        menuPositionInWindow = addMenuCoordinates?.boundsInWindow()?.let { Offset(it.left, it.bottom) },
        menuEntries = persistentListOf(
            DesktopPopupMenuEntry.Action(strings.addFeed, icon = Icons.Outlined.AddCircleOutline, onClick = {
                showAddOptions = false
                onAddFeedClick()
            }),
            DesktopPopupMenuEntry.Action(strings.feedSuggestionsTitle, icon = Icons.Outlined.Lightbulb, onClick = {
                showAddOptions = false
                onFeedSuggestionsClick()
            }),
            DesktopPopupMenuEntry.Action(strings.importFeedButton, icon = Icons.Outlined.FileDownload, onClick = {
                showAddOptions = false
                onImportFeedsClick()
            }),
        ),
        closeMenu = { showAddOptions = false },
    )
    DesktopPopupMenu(
        showMenu = showOptions,
        menuPositionInWindow = menuCoordinates?.boundsInWindow()?.let { Offset(it.left, it.bottom) },
        menuEntries = menuEntries,
        closeMenu = { showOptions = false },
    )
    DrawerDeleteAllFeedsDialog(
        showDialog = showDeleteAllUncategorizedDialog,
        onDismiss = { showDeleteAllUncategorizedDialog = false },
        onDeleteAllFeeds = {
            onDeleteAllFeedsInCategoryClick(uncategorizedSources)
            showDeleteAllUncategorizedDialog = false
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun DesktopDrawerFeedSourceByCategoryItem(
    feedSourceCategoryWrapper: DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper,
    drawerFeedSources: ImmutableList<DrawerItem.DrawerFeedSource>,
    currentFeedFilter: FeedFilter,
    drawerItemVisualStyle: DrawerItemVisualStyle,
    isCategoryExpanded: Boolean,
    onCategoryExpand: () -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    selectedFeedSourceIds: ImmutableSet<String>,
    onFeedSourceClick: (FeedSource, Boolean, Boolean) -> Unit,
    selectedFeedSourcesProvider: () -> List<FeedSource>,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onChangeFeedCategoryClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onDeleteCategoryClick: (CategoryId) -> Unit,
    onDeleteAllFeedsInCategoryClick: (List<FeedSource>) -> Unit,
    onMarkAllReadForFeedSourceClick: (FeedSource) -> Unit,
    onMarkAllReadForCategoryClick: (FeedSourceCategory) -> Unit,
    onMoveFeedSourcesToCategory: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    dragState: DesktopDrawerDragState,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    disclosureModifier: Modifier = Modifier,
    showFeedSources: Boolean = true,
    topSpacing: Dp = 0.dp,
) {
    var headerPosition by remember { mutableStateOf(Offset.Zero) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var menuPositionInWindow by remember { mutableStateOf<Offset?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAllFeedsDialog by remember { mutableStateOf(false) }

    val category = feedSourceCategoryWrapper.feedSourceCategory
    val unreadCount = remember(drawerFeedSources) {
        drawerFeedSources.sumOf { it.unreadCount }
    }
    val isDropTargetActive = dragState.isDragOver(category)

    DrawerDropTargetCleanup(
        dragState = dragState,
        category = category,
    )
    CategoryHeaderSlotCleanup(
        dragState = dragState,
        sectionKey = desktopDrawerCategorySectionKey(category),
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(top = topSpacing),
    ) {
        @Suppress("MagicNumber")
        val degrees by conditionalAnimateFloatAsState(
            targetValue = if (isCategoryExpanded) -90f else 90f,
            animationSpec = spring(),
            label = "Category arrow animation",
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val headerText = category?.title ?: LocalFeedFlowStrings.current.noCategory
            val isSelected = if (category != null) {
                currentFeedFilter is FeedFilter.Category && currentFeedFilter.feedCategory == category
            } else {
                currentFeedFilter is FeedFilter.Uncategorized
            }
            val contentColor = drawerItemVisualStyle.selectedContentColor ?: JewelTheme.globalColors.text.normal
            val itemShape = drawerItemVisualStyle.itemShape
            val interactionSource = remember(category) { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val isFocused by interactionSource.collectIsFocusedAsState()
            val categoryBackground = sidebarRowBackground(
                selected = isSelected,
                highlighted = isHovered || isFocused,
                selectedColor = drawerItemVisualStyle.selectedContainerColor,
            )
            val dropTargetModifier = Modifier.dropTargetModifier(
                dragState = dragState,
                category = category,
                isDropTargetActive = isDropTargetActive,
                highlightColor = desktopDrawerInsertionColor(),
                shape = itemShape,
            )
            val onClick = {
                if (category != null) {
                    onFeedFilterSelected(FeedFilter.Category(feedCategory = category))
                } else {
                    onFeedFilterSelected(FeedFilter.Uncategorized)
                }
            }

            Box(
                modifier = headerModifier
                    .onGloballyPositioned { headerPosition = it.positionInWindow() }
                    .categoryContextMenu(position = { headerPosition }) { position ->
                        menuPositionInWindow = position
                        showMenu = true
                    }
                    .clip(itemShape)
                    .background(categoryBackground)
                    .selectable(
                        selected = isSelected,
                        onClick = onClick,
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = null,
                    )
                    .weight(1f)
                    .semantics { role = Role.Tab }
                    .heightIn(min = drawerItemVisualStyle.itemMinHeight)
                    .hoverable(interactionSource)
                    .then(dropTargetModifier),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val labelColor = contentColor
                    CompositionLocalProvider(LocalContentColor provides labelColor) {
                        Box(Modifier.weight(1f)) {
                            Tooltip(tooltip = { Text(headerText) }) {
                                Text(
                                    text = headerText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    if (unreadCount > 0) {
                        Text(
                            modifier = Modifier.padding(start = DesktopDesignTokens.spaceMedium),
                            text = unreadCount.toString(),
                            style = DesktopDesignTokens.metadataText,
                            color = JewelTheme.globalColors.text.info,
                        )
                    }
                }
            }

            val strings = LocalFeedFlowStrings.current
            val disclosureLabel = if (isCategoryExpanded) {
                strings.desktopCollapseFeedCategory(headerText)
            } else {
                strings.desktopExpandFeedCategory(headerText)
            }
            Tooltip(tooltip = { Text(disclosureLabel) }) {
                IconButton(onClick = onCategoryExpand, modifier = disclosureModifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = disclosureLabel,
                        modifier = Modifier.size(16.dp).rotate(degrees),
                        tint = JewelTheme.globalColors.text.normal,
                    )
                }
            }
        }

        if (showFeedSources) {
            FeedSourcesListWithCategorySelector(
                isCategoryExpanded = isCategoryExpanded,
                drawerFeedSources = drawerFeedSources,
                currentFeedFilter = currentFeedFilter,
                drawerItemVisualStyle = drawerItemVisualStyle,
                selectedFeedSourceIds = selectedFeedSourceIds,
                onFeedSourceClick = onFeedSourceClick,
                selectedFeedSourcesProvider = selectedFeedSourcesProvider,
                onEditFeedClick = onEditFeedClick,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onPinFeedClick = onPinFeedClick,
                onChangeFeedCategoryClick = onChangeFeedCategoryClick,
                onOpenWebsite = onOpenWebsite,
                onMarkAllReadForFeedSourceClick = onMarkAllReadForFeedSourceClick,
                onMoveFeedSourcesToCategory = onMoveFeedSourcesToCategory,
                dragState = dragState,
                sectionKey = desktopDrawerCategorySectionKey(category),
                sectionCategory = category,
            )
        }
    }

    val strings = LocalFeedFlowStrings.current
    val deleteAllFeedsEntry = remember(strings) {
        DesktopPopupMenuEntry.Action(
            text = strings.deleteAllFeedsInCategory,
            onClick = {
                showMenu = false
                menuPositionInWindow = null
                showDeleteAllFeedsDialog = true
            },
        )
    }

    if (category != null) {
        val menuEntries = buildList {
            add(
                DesktopPopupMenuEntry.Action(
                    text = strings.renameCategory,
                    onClick = {
                        showMenu = false
                        menuPositionInWindow = null
                        showEditDialog = true
                    },
                ),
            )

            if (unreadCount > 0) {
                add(
                    DesktopPopupMenuEntry.Action(
                        text = strings.markAllReadButton,
                        onClick = {
                            showMenu = false
                            menuPositionInWindow = null
                            onMarkAllReadForCategoryClick(category)
                        },
                    ),
                )
            }

            add(DesktopPopupMenuEntry.Divider)
            add(deleteAllFeedsEntry)
            add(
                DesktopPopupMenuEntry.Action(
                    text = strings.deleteCategory,
                    onClick = {
                        showMenu = false
                        menuPositionInWindow = null
                        showDeleteDialog = true
                    },
                ),
            )
        }.toImmutableList()

        DesktopPopupMenu(
            showMenu = showMenu,
            menuPositionInWindow = menuPositionInWindow,
            menuEntries = menuEntries,
            closeMenu = {
                showMenu = false
                menuPositionInWindow = null
            },
        )

        DrawerDeleteCategoryDialog(
            showDialog = showDeleteDialog,
            categoryId = CategoryId(category.id),
            onDismiss = { showDeleteDialog = false },
            onDeleteCategory = { id ->
                onDeleteCategoryClick(id)
                showDeleteDialog = false
            },
        )

        DrawerEditCategoryNameDialog(
            showDialog = showEditDialog,
            categoryId = CategoryId(category.id),
            initialCategoryName = category.title,
            onDismiss = { showEditDialog = false },
            validateCategoryName = validateCategoryName,
            onEditCategory = onEditCategoryClick,
        )
    } else {
        val menuEntries = remember(deleteAllFeedsEntry) {
            persistentListOf(deleteAllFeedsEntry)
        }

        DesktopPopupMenu(
            showMenu = showMenu,
            menuPositionInWindow = menuPositionInWindow,
            menuEntries = menuEntries,
            closeMenu = {
                showMenu = false
                menuPositionInWindow = null
            },
        )
    }

    DrawerDeleteAllFeedsDialog(
        showDialog = showDeleteAllFeedsDialog,
        onDismiss = { showDeleteAllFeedsDialog = false },
        onDeleteAllFeeds = {
            onDeleteAllFeedsInCategoryClick(drawerFeedSources.map { it.feedSource })
            showDeleteAllFeedsDialog = false
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.categoryContextMenu(position: () -> Offset, onShow: (Offset?) -> Unit): Modifier =
    onPointerEvent(PointerEventType.Press) { event ->
        if (event.buttons.isSecondaryPressed) {
            onShow(position() + (event.changes.firstOrNull()?.position ?: Offset.Zero))
            event.changes.forEach { it.consume() }
        }
    }.onPreviewKeyEvent { event ->
        val contextKey = event.key == Key.Menu || (event.key == Key.F10 && event.isShiftPressed)
        if (event.type == KeyEventType.KeyDown && contextKey) {
            onShow(position())
            true
        } else {
            false
        }
    }
