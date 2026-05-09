package com.prof18.feedflow.android.feedsuggestions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import com.prof18.feedflow.shared.ui.feedsuggestions.FeedSuggestionsContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedSuggestionsScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedSuggestionsViewModel = koinViewModel(),
) {
    val categories by viewModel.suggestedCategoriesState.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryIdState.collectAsStateWithLifecycle()
    val feedStatesMap by viewModel.feedStatesMapState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingState.collectAsStateWithLifecycle()

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
        modifier = modifier,
    ) { innerPadding ->
        FeedSuggestionsContent(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            feedStatesMap = feedStatesMap,
            isLoading = isLoading,
            onCategorySelected = viewModel::selectCategory,
            onAddFeed = { feed, categoryName ->
                viewModel.addFeed(feed, categoryName)
            },
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        )
    }
}
