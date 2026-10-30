package com.prof18.feedflow.desktop.addfeed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.feedforms.AddFeedForm
import com.prof18.feedflow.desktop.feedforms.FeedCategoryActions
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AddFeedScreen(onCloseRequest: () -> Unit, onFeedAdded: () -> Unit) {
    AddFeedWindow(onCloseRequest) { AddFeedScreenContent(onFeedAdded, onCancel = onCloseRequest) }
}

@Composable
internal fun AddFeedWindow(onCloseRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        title = LocalFeedFlowStrings.current.addFeed,
        size = DpSize(520.dp, 330.dp),
        onCloseRequest = onCloseRequest,
    ) { content() }
}

@Composable
fun AddFeedScreenContent(
    onFeedAdded: () -> Unit,
    modifier: Modifier = Modifier,
    topAppBar: @Composable () -> Unit = {},
    onCancel: (() -> Unit)? = null,
) {
    var feedUrl by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var completionRevision by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf("") }
    var canForceAdd by remember { mutableStateOf(false) }
    val viewModel = koinViewModel<AddFeedViewModel>()
    val latestOnFeedAdded by rememberUpdatedState(onFeedAdded)
    val snackbarHostState = remember { DesktopFeedbackState() }
    val strings = LocalFeedFlowStrings.current
    LaunchedEffect(Unit) {
        viewModel.feedAddedState.collect { state ->
            when (state) {
                is FeedAddedState.Error -> {
                    completionRevision++
                    showLoading = false
                    showError = true
                    canForceAdd = state.canForceAdd
                    errorMessage = when (state) {
                        is FeedAddedState.Error.InvalidUrl -> strings.invalidRssUrlWithRetryHint
                        is FeedAddedState.Error.InvalidTitleLink -> strings.missingTitleAndLink
                        is FeedAddedState.Error.GenericError -> strings.addFeedGenericError
                        is FeedAddedState.Error.FetchFailed -> strings.feedFetchRefusedError
                    }
                }
                is FeedAddedState.FeedAdded, is FeedAddedState.FeedAlreadyExists -> {
                    val addedState = state as? FeedAddedState.FeedAdded
                    val feedName = addedState?.feedName
                    val message = if (state is FeedAddedState.FeedAlreadyExists) {
                        strings.feedAlreadyExistsMessage(state.feedName)
                    } else if (addedState?.shortsFilterFallback == true) {
                        strings.hideYoutubeShortsFallback
                    } else if (feedName != null) {
                        strings.feedAddedMessage(feedName)
                    } else {
                        strings.feedAddedMessageWithoutName
                    }
                    snackbarHostState.showFeedback(message, duration = DesktopFeedbackDuration.Short)
                    showLoading = false
                    feedUrl = ""
                    viewModel.updateFeedUrlTextFieldValue(feedUrl)
                    showError = false
                    errorMessage = ""
                    canForceAdd = false
                    completionRevision++
                    latestOnFeedAdded()
                }
                FeedAddedState.FeedNotAdded -> {
                    showLoading = false
                    showError = false
                    errorMessage = ""
                    canForceAdd = false
                }
                FeedAddedState.Loading -> showLoading = true
            }
        }
    }
    val categories by viewModel.categoriesState.collectAsState()
    val showNotification by viewModel.showNotificationToggleState.collectAsState()
    val notificationEnabled by viewModel.isNotificationEnabledState.collectAsState()
    val youTubeOptions by viewModel.youTubeOptionsState.collectAsState()
    AddFeedForm(
        feedUrl = feedUrl,
        showError = showError,
        showLoading = showLoading,
        errorMessage = errorMessage,
        categoriesState = categories,
        showNotificationToggle = showNotification,
        isNotificationEnabled = notificationEnabled,
        canForceAdd = canForceAdd,
        onFeedUrlUpdated = {
            feedUrl = it
            viewModel.updateFeedUrlTextFieldValue(it)
        },
        onAddFeed = viewModel::addFeed,
        categoryActions = FeedCategoryActions(
            onSelect = viewModel::onCategorySelected,
            onAdd = viewModel::addNewCategory,
            validate = viewModel::validateCategoryName,
        ),
        onCancel = onCancel,
        onNotificationToggleChanged = viewModel::updateNotificationStatus,
        onForceAddFeed = viewModel::forceAddFeed,
        modifier = modifier,
        youTubeOptions = youTubeOptions,
        onHideShortsToggleChanged = viewModel::updateHideShortsStatus,
        completionRevision = completionRevision,
        topAppBar = topAppBar,
        snackbarHostState = snackbarHostState,
    )
}
