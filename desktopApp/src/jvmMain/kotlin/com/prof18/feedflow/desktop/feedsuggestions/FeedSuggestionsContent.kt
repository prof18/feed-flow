package com.prof18.feedflow.desktop.feedsuggestions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.desktop.home.drawer.sidebarRowBackground
import com.prof18.feedflow.desktop.settings.SettingsChoice
import com.prof18.feedflow.desktop.testing.FeedSuggestionsE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.desktop.ui.components.LocalWindowTopInset
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationState
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionUndoOffer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun FeedSuggestionsContent(
    categories: ImmutableList<SuggestedFeedCategory>,
    selectedCategoryId: String?,
    isLoading: Boolean,
    onCategorySelected: (String) -> Unit,
    onAddFeed: (SuggestedFeed, String) -> Unit,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    operationStates: ImmutableMap<String, FeedSuggestionOperationState> = persistentMapOf(),
    undoOfferState: FeedSuggestionUndoOffer? = null,
    onUndo: (Long) -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    val selected = categories.find { it.id == selectedCategoryId } ?: categories.firstOrNull()
    val categoryState = rememberLazyListState()
    val feedState = rememberLazyListState()
    val categoryFocus = remember { FocusRequester() }
    val closeFocus = remember { FocusRequester() }
    val topInset = LocalWindowTopInset.current
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag(FeedSuggestionsE2eIds.SCREEN)) {
        val wide = maxWidth >= TwoPaneMinWidth
        val showCategories = !isLoading && selected != null
        LaunchedEffect(selected?.id, isLoading, wide) {
            if (!isLoading && selected != null) {
                if (wide) {
                    categoryState.scrollToItem(categories.indexOf(selected))
                    categoryFocus.requestFocus()
                }
                feedState.scrollToItem(0)
            } else if (!isLoading) {
                closeFocus.requestFocus()
            }
        }
        Row(Modifier.fillMaxSize()) {
            if (wide && showCategories) {
                SuggestionCategoryPane(categories, selected, categoryState, categoryFocus, topInset, onCategorySelected)
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .padding(top = topInset)
                    .padding(DesktopDesignTokens.spacePanel),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
            ) {
                if (showCategories && !wide) {
                    SettingsChoice(
                        title = strings.addFeedCategoryTitle,
                        choices = categories.map { it.id to "${it.icon}  ${it.name}" }.toPersistentList(),
                        selected = selected.id,
                        onSelected = onCategorySelected,
                    )
                }
                if (showCategories && wide) {
                    Column(verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSmall)) {
                        Text(
                            "${selected.icon}  ${selected.name}",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                            ),
                        )
                        Text(
                            strings.feedSuggestionsDescription,
                            color = JewelTheme.globalColors.text.info,
                        )
                    }
                } else {
                    Text(
                        strings.feedSuggestionsDescription,
                        color = JewelTheme.globalColors.text.info,
                    )
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    SuggestionFeedArea(isLoading, selected, feedState, operationStates, onAddFeed)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
                ) {
                    SuggestionUndoStatus(
                        offer = undoOfferState,
                        onUndo = onUndo,
                        modifier = Modifier.weight(1f),
                    )
                    DefaultButton(
                        onClick = onCloseRequest,
                        modifier = Modifier.heightIn(min = suggestionActionHeight()).focusRequester(closeFocus),
                    ) {
                        Text(strings.actionDone)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCategoryPane(
    categories: ImmutableList<SuggestedFeedCategory>,
    selected: SuggestedFeedCategory,
    state: LazyListState,
    selectedFocus: FocusRequester,
    topInset: Dp,
    onCategorySelected: (String) -> Unit,
) {
    LazyColumn(
        state = state,
        modifier = Modifier
            .width(SuggestionCategoryPaneWidth)
            .fillMaxHeight()
            .background(JewelTheme.globalColors.toolwindowBackground)
            .padding(top = topInset)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            SuggestionCategory(
                category = category,
                isSelected = category.id == selected.id,
                onSelect = { onCategorySelected(category.id) },
                modifier = if (category.id == selected.id) Modifier.focusRequester(selectedFocus) else Modifier,
            )
        }
    }
}

@Composable
private fun SuggestionFeedArea(
    isLoading: Boolean,
    selected: SuggestedFeedCategory?,
    feedState: LazyListState,
    operationStates: ImmutableMap<String, FeedSuggestionOperationState>,
    onAddFeed: (SuggestedFeed, String) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    when {
        isLoading -> CircularProgressIndicator()
        selected == null -> Text(strings.feedSuggestionsEmptyCatalog)
        selected.feeds.isEmpty() -> Text(strings.feedSuggestionsEmptyCategory)
        else -> Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = feedState,
                modifier = Modifier.fillMaxSize().padding(end = DesktopDesignTokens.spaceSection),
                contentPadding = PaddingValues(vertical = DesktopDesignTokens.spaceMedium),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
            ) {
                items(selected.feeds, key = { it.url }) { feed ->
                    SuggestedFeedRow(
                        feed = feed,
                        selectedCategoryName = selected.name,
                        operation = operationStates[feed.url],
                        onAdd = { onAddFeed(feed, selected.name) },
                    )
                }
            }
            if (feedState.canScrollForward || feedState.canScrollBackward) {
                FeedFlowVerticalScrollbar(feedState)
            }
        }
    }
}

@Composable
private fun SuggestionCategory(
    category: SuggestedFeedCategory,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(DesktopDesignTokens.buttonCornerSize))
            .background(sidebarRowBackground(selected = isSelected, highlighted = hovered || focused))
            .hoverable(interactionSource)
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .testTag(FeedSuggestionsE2eIds.category(category.id))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(category.icon)
        Text(
            category.name,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SuggestedFeedRow(
    feed: SuggestedFeed,
    selectedCategoryName: String,
    operation: FeedSuggestionOperationState?,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().testTag(FeedSuggestionsE2eIds.row(feed.url))) {
        if (maxWidth < WideSuggestionRowWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium)) {
                SuggestedFeedDetails(feed, operation, selectedCategoryName)
                SuggestedFeedAction(feed, operation?.status, onAdd, Modifier.align(Alignment.End))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection)) {
                SuggestedFeedDetails(feed, operation, selectedCategoryName, Modifier.weight(1f))
                SuggestedFeedAction(feed, operation?.status, onAdd)
            }
        }
    }
}

@Composable
private fun SuggestedFeedStatus(operation: FeedSuggestionOperationState?, selectedCategoryName: String) {
    if (operation == null) return
    val strings = LocalFeedFlowStrings.current
    val failed = operation.status == FeedSuggestionOperationStatus.AddFailed
    val errorMessage = if (failed) strings.feedSuggestionAddError else null
    val addedToCategory = operation.addedToCategory?.takeIf { it != selectedCategoryName }
    val addedTo = addedToCategory?.let { strings.feedSuggestionAddedTo(it) }
    if (errorMessage == null && addedTo == null) return

    Column(
        modifier = Modifier.padding(top = DesktopDesignTokens.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSmall),
    ) {
        if (addedTo != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSmall),
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = JewelTheme.globalColors.text.info,
                )
                Text(addedTo, style = DesktopDesignTokens.metadataText, color = JewelTheme.globalColors.text.info)
            }
        }
        if (errorMessage != null) {
            SuggestedFeedError(errorMessage)
        }
    }
}

@Composable
private fun SuggestedFeedError(message: String) {
    Text(
        message,
        modifier = Modifier.semantics {
            error(message)
            liveRegion = LiveRegionMode.Polite
        },
        style = DesktopDesignTokens.metadataText,
        color = JewelTheme.globalColors.text.error,
    )
}

@Composable
private fun SuggestionUndoStatus(
    offer: FeedSuggestionUndoOffer?,
    onUndo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    var undoneGeneration by remember { mutableStateOf<Long?>(null) }
    val visibleOffer = offer?.takeIf { it.generation != undoneGeneration }
    Box(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        if (visibleOffer != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceLarge),
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = JewelTheme.globalColors.text.info,
                )
                Text(
                    strings.feedSuggestionAddedMessage(visibleOffer.feedName, visibleOffer.categoryName),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedButton(
                    onClick = {
                        undoneGeneration = visibleOffer.generation
                        onUndo(visibleOffer.generation)
                    },
                    modifier = Modifier.heightIn(min = suggestionActionHeight())
                        .testTag("feed_suggestion_undo")
                        .semantics {
                            contentDescription = "${strings.feedSuggestionUndo} · ${visibleOffer.feedName}"
                        },
                ) {
                    Text(strings.feedSuggestionUndo)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SuggestedFeedDetails(
    feed: SuggestedFeed,
    operation: FeedSuggestionOperationState?,
    selectedCategoryName: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection)) {
        Box(
            modifier = Modifier.size(SuggestionLogoSize).clip(RoundedCornerShape(DesktopDesignTokens.spaceMedium)),
            contentAlignment = Alignment.Center,
        ) {
            if (feed.logoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current).data(feed.logoUrl).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(AllIconsKeys.General.Language, null)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSmall)) {
            Tooltip(tooltip = { Text(feed.name) }) {
                Text(feed.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Tooltip(tooltip = { Text(feed.description) }) {
                Text(feed.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Tooltip(tooltip = { Text(feed.url) }) {
                Text(
                    text = feed.url,
                    color = JewelTheme.globalColors.text.info,
                    style = DesktopDesignTokens.metadataText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SuggestedFeedStatus(operation, selectedCategoryName)
        }
    }
}

@Composable
private fun SuggestedFeedAction(
    feed: SuggestedFeed,
    status: FeedSuggestionOperationStatus?,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val canAdd = status == null || status == FeedSuggestionOperationStatus.AddFailed
    val currentCanAdd by rememberUpdatedState(canAdd)
    val add by rememberUpdatedState(onAdd)
    val label = when (status) {
        FeedSuggestionOperationStatus.Adding -> strings.addingFeed
        FeedSuggestionOperationStatus.Added -> strings.feedSuggestionAdded
        FeedSuggestionOperationStatus.AddFailed -> strings.retryButton
        null -> strings.feedSuggestionAdd
    }
    OutlinedButton(
        onClick = { if (currentCanAdd) add() },
        enabled = canAdd,
        modifier = modifier.heightIn(min = suggestionActionHeight())
            .testTag(FeedSuggestionsE2eIds.addButton(feed.url))
            .semantics { contentDescription = "$label · ${feed.name}" },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
        ) {
            when (status) {
                FeedSuggestionOperationStatus.Adding -> CircularProgressIndicator()
                FeedSuggestionOperationStatus.Added -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                else -> Unit
            }
            Text(label)
        }
    }
}

@Composable
@ReadOnlyComposable
private fun suggestionActionHeight(): Dp =
    with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp

private val SuggestionCategoryPaneWidth = 200.dp
private val TwoPaneMinWidth = 560.dp
private val WideSuggestionRowWidth = 560.dp
private val SuggestionLogoSize = 40.dp
