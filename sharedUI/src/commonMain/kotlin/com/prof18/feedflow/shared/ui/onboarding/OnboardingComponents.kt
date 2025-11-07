package com.prof18.feedflow.shared.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.ui.style.Spacing

@Composable
fun OnboardingContent(
    categories: List<SuggestedFeedCategory>,
    selectedFeeds: Set<String>,
    expandedCategories: Set<String>,
    isLoading: Boolean,
    onFeedToggle: (String) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            OnboardingHeader(
                modifier = Modifier.fillMaxWidth(),
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.regular),
                verticalArrangement = Arrangement.spacedBy(Spacing.regular),
            ) {
                items(
                    items = categories,
                    key = { it.id },
                ) { category ->
                    CategoryCard(
                        category = category,
                        isExpanded = expandedCategories.contains(category.id),
                        selectedFeeds = selectedFeeds,
                        onCategoryToggle = { onCategoryToggle(category.id) },
                        onFeedToggle = onFeedToggle,
                    )
                }
            }

            OnboardingFooter(
                selectedFeedsCount = selectedFeeds.size,
                isLoading = isLoading,
                onComplete = onComplete,
                onSkip = onSkip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.regular),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Welcome to FeedFlow",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Get started by selecting some feeds to follow",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.small),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
    category: SuggestedFeedCategory,
    isExpanded: Boolean,
    selectedFeeds: Set<String>,
    onCategoryToggle: () -> Unit,
    onFeedToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                onClick = onCategoryToggle,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.regular),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        category.icon?.let { icon ->
                            Text(
                                text = icon,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            val selectedInCategory = category.feeds.count { selectedFeeds.contains(it.url) }
                            if (selectedInCategory > 0) {
                                Text(
                                    text = "$selectedInCategory selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                FeedList(
                    feeds = category.feeds,
                    selectedFeeds = selectedFeeds,
                    onFeedToggle = onFeedToggle,
                    modifier = Modifier.padding(
                        start = Spacing.regular,
                        end = Spacing.regular,
                        bottom = Spacing.regular,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FeedList(
    feeds: List<SuggestedFeed>,
    selectedFeeds: Set<String>,
    onFeedToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        feeds.forEach { feed ->
            val isSelected = selectedFeeds.contains(feed.url)
            FilterChip(
                selected = isSelected,
                onClick = { onFeedToggle(feed.url) },
                label = {
                    Text(
                        text = feed.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun OnboardingFooter(
    selectedFeedsCount: Int,
    isLoading: Boolean,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.regular),
        ) {
            if (selectedFeedsCount > 0) {
                Text(
                    text = "$selectedFeedsCount feed${if (selectedFeedsCount > 1) "s" else ""} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.small),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
            ) {
                TextButton(
                    onClick = onSkip,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Skip")
                }

                Button(
                    onClick = onComplete,
                    enabled = selectedFeedsCount > 0 && !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSuggestionsContent(
    categories: List<SuggestedFeedCategory>,
    selectedFeeds: Set<String>,
    expandedCategories: Set<String>,
    isLoading: Boolean,
    onFeedToggle: (String) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onAddFeeds: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed Suggestions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = "Discover and subscribe to curated feed suggestions",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.regular, vertical = Spacing.small),
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(Spacing.regular),
                    verticalArrangement = Arrangement.spacedBy(Spacing.regular),
                ) {
                    items(
                        items = categories,
                        key = { it.id },
                    ) { category ->
                        CategoryCard(
                            category = category,
                            isExpanded = expandedCategories.contains(category.id),
                            selectedFeeds = selectedFeeds,
                            onCategoryToggle = { onCategoryToggle(category.id) },
                            onFeedToggle = onFeedToggle,
                        )
                    }
                }

                FeedSuggestionsFooter(
                    selectedFeedsCount = selectedFeeds.size,
                    isLoading = isLoading,
                    onAddFeeds = onAddFeeds,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FeedSuggestionsFooter(
    selectedFeedsCount: Int,
    isLoading: Boolean,
    onAddFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.regular),
        ) {
            if (selectedFeedsCount > 0) {
                Text(
                    text = "$selectedFeedsCount feed${if (selectedFeedsCount > 1) "s" else ""} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.small),
                )
            }

            Button(
                onClick = onAddFeeds,
                enabled = selectedFeedsCount > 0 && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Add Selected Feeds")
                }
            }
        }
    }
}
