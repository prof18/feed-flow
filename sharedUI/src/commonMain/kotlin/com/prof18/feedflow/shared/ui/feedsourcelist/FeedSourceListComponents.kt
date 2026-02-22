package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.conditionalAnimateFloatAsState
import kotlinx.collections.immutable.ImmutableList

expect fun Modifier.singleAndLongClickModifier(
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)?,
): Modifier

@Composable
internal fun FeedSourcesWithCategoryList(
    paddingValues: PaddingValues,
    feedSourceState: FeedSourceListState,
    onExpandClicked: (CategoryId?) -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onRenameFeedSourceClick: (FeedSource, String) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onDeleteAllFeedsInCategory: (List<FeedSource>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDir = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier
            .padding(top = paddingValues.calculateTopPadding())
            .padding(start = paddingValues.calculateLeftPadding(layoutDir))
            .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        contentPadding = PaddingValues(Spacing.regular),
    ) {
        item {
            FeedSourcesList(
                feedSources = feedSourceState.feedSourcesWithoutCategory,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onRenameFeedSourceClick = onRenameFeedSourceClick,
                onEditFeedClick = onEditFeedClick,
                onPinFeedClick = onPinFeedClick,
                onOpenWebsite = onOpenWebsite,
            )
        }

        items(feedSourceState.feedSourcesWithCategory) { feedSourceState ->
            Column {
                var showCategoryMenu by remember { mutableStateOf(false) }

                @Suppress("MagicNumber")
                val degrees by conditionalAnimateFloatAsState(
                    targetValue = if (feedSourceState.isExpanded) {
                        -90f
                    } else {
                        90f
                    },
                    animationSpec = spring(),
                )
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .singleAndLongClickModifier(
                            onClick = {
                                onExpandClicked(feedSourceState.categoryId)
                            },
                            onLongClick = {
                                showCategoryMenu = true
                            },
                        )
                        .fillMaxWidth()
                        .padding(vertical = Spacing.regular),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val headerText = if (feedSourceState.categoryName != null) {
                        requireNotNull(feedSourceState.categoryName)
                    } else {
                        LocalFeedFlowStrings.current.noCategory
                    }

                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.rotate(degrees),
                    )
                }

                CategoryHeaderContextMenu(
                    showMenu = showCategoryMenu,
                    feedSources = feedSourceState.feedSources,
                    hideMenu = { showCategoryMenu = false },
                    onDeleteAllFeedsClick = onDeleteAllFeedsInCategory,
                )

                FeedSourcesListWithCategorySelector(
                    feedSourceState = feedSourceState,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                    onRenameFeedSourceClick = onRenameFeedSourceClick,
                    onEditFeedClick = onEditFeedClick,
                    onPinFeedClick = onPinFeedClick,
                    onOpenWebsite = onOpenWebsite,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
private fun FeedSourcesListWithCategorySelector(
    feedSourceState: FeedSourceState,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onRenameFeedSourceClick: (FeedSource, String) -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
) {
    ConditionalAnimatedVisibility(
        visible = feedSourceState.isExpanded,
        enter = expandVertically(
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
        ),
        exit = shrinkVertically(),
    ) {
        FeedSourcesList(
            feedSources = feedSourceState.feedSources,
            onDeleteFeedSourceClick = onDeleteFeedSourceClick,
            onRenameFeedSourceClick = onRenameFeedSourceClick,
            onEditFeedClick = onEditFeedClick,
            onPinFeedClick = onPinFeedClick,
            onOpenWebsite = onOpenWebsite,
        )
    }
}

@Composable
private fun FeedSourcesList(
    feedSources: ImmutableList<FeedSource>,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onRenameFeedSourceClick: (FeedSource, String) -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
) {
    Column {
        feedSources.forEachIndexed { index, feedSource ->
            FeedSourceItem(
                feedSource = feedSource,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onRenameFeedSourceClick = onRenameFeedSourceClick,
                onEditFeedClick = onEditFeedClick,
                onPinFeedClick = onPinFeedClick,
                onOpenWebsite = onOpenWebsite,
            )

            if (index < feedSources.size - 1) {
                HorizontalDivider(
                    modifier = Modifier,
                    thickness = 0.2.dp,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun FeedSourceItem(
    feedSource: FeedSource,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onRenameFeedSourceClick: (FeedSource, String) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
) {
    var showFeedMenu by remember {
        mutableStateOf(
            false,
        )
    }

    var feedTitleInput by remember(feedSource.title) {
        mutableStateOf(TextFieldValue(feedSource.title))
    }

    var isEditEnabled by remember {
        mutableStateOf(false)
    }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditEnabled) {
        if (isEditEnabled) {
            focusRequester.requestFocus()
            feedTitleInput = feedTitleInput.copy(
                selection = TextRange(feedTitleInput.text.length),
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (feedSource.fetchFailed) {
            val tooltipText = LocalFeedFlowStrings.current.feedFetchFailedTooltip
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    PlainTooltip { Text(tooltipText) }
                },
                state = rememberTooltipState(),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = tooltipText,
                    tint = Color(color = 0xFFFF8F00),
                )
            }
        }

        val paddingStart = if (feedSource.fetchFailed) Spacing.small else 0.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .singleAndLongClickModifier(
                    onLongClick = if (isEditEnabled) {
                        null
                    } else {
                        {
                            showFeedMenu = true
                        }
                    },
                )
                .padding(start = paddingStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imageUrl = feedSource.logoUrl
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

            Column(
                modifier = Modifier
                    .padding(start = Spacing.regular),
            ) {
                ConditionalAnimatedVisibility(!isEditEnabled) {
                    Text(
                        modifier = Modifier
                            .padding(top = Spacing.small),
                        text = feedSource.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                val interactionSource = remember { MutableInteractionSource() }

                ConditionalAnimatedVisibility(isEditEnabled) {
                    FeedSourceTitleEdit(
                        focusRequester = focusRequester,
                        feedTitleInput = feedTitleInput,
                        isEditEnabled = isEditEnabled,
                        onFeedNameUpdated = {
                            feedTitleInput = it
                        },
                        onRenameFeedSourceClick = {
                            onRenameFeedSourceClick(feedSource, feedTitleInput.text)
                            isEditEnabled = false
                            focusManager.clearFocus()
                        },
                        interactionSource = interactionSource,
                    )
                }

                Text(
                    modifier = Modifier
                        .padding(top = Spacing.xsmall)
                        .padding(bottom = Spacing.small),
                    text = feedSource.url,
                    style = MaterialTheme.typography.labelLarge,
                )

                FeedSourceContextMenu(
                    showFeedMenu = showFeedMenu,
                    hideMenu = {
                        showFeedMenu = false
                    },
                    onEditFeedClick = onEditFeedClick,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                    feedSource = feedSource,
                    onRenameFeedSourceClick = {
                        isEditEnabled = true
                    },
                    onPinFeedClick = onPinFeedClick,
                    onOpenWebsite = onOpenWebsite,
                )
            }
        }
    }
}

@Composable
private fun FeedSourceTitleEdit(
    focusRequester: FocusRequester,
    feedTitleInput: TextFieldValue,
    isEditEnabled: Boolean,
    onFeedNameUpdated: (TextFieldValue) -> Unit,
    onRenameFeedSourceClick: () -> Unit,
    interactionSource: MutableInteractionSource,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .padding(top = Spacing.small),
            value = feedTitleInput,
            onValueChange = onFeedNameUpdated,
            keyboardActions = KeyboardActions(
                onDone = {
                    onRenameFeedSourceClick()
                },
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors().copy(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
            enabled = isEditEnabled,
            interactionSource = interactionSource,
        )

        IconButton(
            onClick = {
                onRenameFeedSourceClick()
            },
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun FeedSourceNavBar(
    navigateBack: () -> Unit,
    onAddFeedSourceClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(LocalFeedFlowStrings.current.feedsTitle)
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navigateBack()
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    onAddFeedSourceClick()
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
internal fun NoFeedSourcesView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier
                .padding(Spacing.regular),
            text = LocalFeedFlowStrings.current.noFeedsAddOneMessage,
        )
    }
}
