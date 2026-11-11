package com.prof18.feedflow.android.ui.feedsuggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationState
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun FeedSuggestionsContent(
    categories: ImmutableList<SuggestedFeedCategory>,
    selectedCategoryId: String?,
    operationStates: ImmutableMap<String, FeedSuggestionOperationState>,
    isLoading: Boolean,
    onCategorySelected: (String) -> Unit,
    onAddFeed: (SuggestedFeed, String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val strings = LocalFeedFlowStrings.current
    val selectedCategory = selectedCategoryId?.let { categoryId ->
        categories.find { it.id == categoryId }
    } ?: categories.firstOrNull()
    val filteredFeeds = selectedCategory?.feeds.orEmpty()

    if (isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(FeedSuggestionsE2eIds.SCREEN)
            .padding(contentPadding),
    ) {
        Text(
            text = strings.feedSuggestionsDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = Spacing.regular)
                .padding(top = Spacing.xsmall, bottom = Spacing.regular),
        )

        CategoryFilterRow(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = Spacing.regular),
        )

        if (categories.isEmpty()) {
            EmptySuggestionsState(
                message = strings.feedSuggestionsEmptyCatalog,
                modifier = Modifier.weight(1f),
            )
        } else if (filteredFeeds.isEmpty()) {
            EmptySuggestionsState(
                message = strings.feedSuggestionsEmptyCategory,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = Spacing.small, bottom = 88.dp),
            ) {
                items(
                    items = filteredFeeds,
                    key = { it.url },
                ) { feed ->
                    val operation = operationStates[feed.url]
                    SuggestedFeedListItem(
                        feed = feed,
                        operation = operation,
                        selectedCategoryName = selectedCategory?.name,
                        onAction = { selectedCategory?.name?.let { onAddFeed(feed, it) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySuggestionsState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CategoryFilterRow(
    categories: ImmutableList<SuggestedFeedCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        items(
            items = categories,
            key = { it.id },
        ) { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
                label = { Text(text = category.name) },
                modifier = Modifier.testTag(FeedSuggestionsE2eIds.category(category.id)),
                leadingIcon = {
                    Text(text = category.icon, style = MaterialTheme.typography.bodyMedium)
                },
            )
        }
    }
}

@Composable
private fun SuggestedFeedListItem(
    feed: SuggestedFeed,
    operation: FeedSuggestionOperationState?,
    selectedCategoryName: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val actionLabel = when (operation?.status) {
        FeedSuggestionOperationStatus.Adding -> strings.addingFeed
        FeedSuggestionOperationStatus.AddFailed -> strings.retryButton
        FeedSuggestionOperationStatus.Added -> strings.feedSuggestionAdded
        null -> strings.feedSuggestionAdd
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(FeedSuggestionsE2eIds.row(feed.url))
            .padding(horizontal = Spacing.regular, vertical = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        val logo = feed.logoUrl
        if (logo != null) {
            FeedSourceLogoImage(imageUrl = logo, size = 40.dp)
        } else {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Spacing.small))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(Spacing.small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = feed.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = feed.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            SuggestedFeedFeedback(operation, selectedCategoryName)
        }

        AddButton(
            status = operation?.status,
            actionLabel = actionLabel,
            feedName = feed.name,
            onClick = onAction,
            modifier = Modifier.testTag(FeedSuggestionsE2eIds.addButton(feed.url)),
        )
    }
}

@Composable
private fun SuggestedFeedFeedback(operation: FeedSuggestionOperationState?, selectedCategoryName: String?) {
    val strings = LocalFeedFlowStrings.current
    if (operation == null) return
    val addedToCategory = operation.addedToCategory?.takeIf { it != selectedCategoryName }

    Column {
        if (addedToCategory != null) {
            Row(
                modifier = Modifier.padding(top = Spacing.xsmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xsmall),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = strings.feedSuggestionAddedTo(addedToCategory),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (operation.status == FeedSuggestionOperationStatus.AddFailed) {
            FeedbackError(strings.feedSuggestionAddError)
        }
    }
}

@Composable
private fun FeedbackError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .padding(top = Spacing.xsmall)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun AddButton(
    status: FeedSuggestionOperationStatus?,
    actionLabel: String,
    feedName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val actionDescription = "$actionLabel · $feedName"
    when (status) {
        FeedSuggestionOperationStatus.Adding -> {
            OutlinedButton(
                onClick = {},
                modifier = modifier.semantics { contentDescription = actionDescription },
                enabled = false,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                CircularProgressIndicator(
                    color = LocalContentColor.current,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        FeedSuggestionOperationStatus.Added -> {
            FilledTonalButton(
                onClick = {},
                modifier = modifier.semantics { contentDescription = actionDescription },
                enabled = false,
                colors = ButtonDefaults.filledTonalButtonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.feedSuggestionAdded, style = MaterialTheme.typography.labelMedium)
            }
        }
        FeedSuggestionOperationStatus.AddFailed, null -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = actionDescription },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                if (status == null) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
