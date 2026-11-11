package com.prof18.feedflow.android.feedsuggestions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.ui.feedsuggestions.FeedSuggestionsContent
import com.prof18.feedflow.android.ui.feedsuggestions.FeedSuggestionsE2eIds
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedSuggestionsScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedSuggestionsViewModel = koinViewModel(),
) {
    val categories by viewModel.suggestedCategoriesState.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryIdState.collectAsStateWithLifecycle()
    val operationStates by viewModel.operationStates.collectAsStateWithLifecycle()
    val undoOfferState by viewModel.undoOfferState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingState.collectAsStateWithLifecycle()
    val accessibilityManager = LocalAccessibilityManager.current
    val undoTimeoutMillis = accessibilityManager
        ?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = 10_000L,
            containsIcons = true,
            containsText = true,
            containsControls = true,
        )
        ?.coerceAtLeast(10_000L)
        ?: 10_000L

    val strings = LocalFeedFlowStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUndoOffer = undoOfferState
    LaunchedEffect(currentUndoOffer) {
        if (currentUndoOffer == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val result = snackbarHostState.showSnackbar(
            message = strings.feedSuggestionAddedMessage(currentUndoOffer.feedName, currentUndoOffer.categoryName),
            actionLabel = strings.feedSuggestionUndo,
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastAdd(currentUndoOffer.generation)
        }
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.endVisit() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(LocalFeedFlowStrings.current.feedSuggestionsTitle)
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.testTag(FeedSuggestionsE2eIds.UNDO_SNACKBAR),
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        FeedSuggestionsContent(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            operationStates = operationStates,
            isLoading = isLoading,
            onCategorySelected = viewModel::selectCategory,
            onAddFeed = { feed, categoryName ->
                viewModel.addFeed(feed, categoryName, undoTimeoutMillis)
            },
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        )
    }
}
