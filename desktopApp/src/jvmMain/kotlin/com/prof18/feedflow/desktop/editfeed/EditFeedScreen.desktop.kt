package com.prof18.feedflow.desktop.editfeed

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
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.desktop.feedforms.EditFeedForm
import com.prof18.feedflow.desktop.feedforms.FeedCategoryActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ArticleOpenModeSelector
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun EditFeedScreen(feedSource: FeedSource?, onCloseRequest: () -> Unit) {
    if (feedSource == null) return
    EditFeedWindow(onCloseRequest) { EditFeedScreenContent(feedSource, onCloseRequest) }
}

@Composable
internal fun EditFeedWindow(onCloseRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        title = LocalFeedFlowStrings.current.openFeedSettings,
        size = DpSize(560.dp, 560.dp),
        onCloseRequest = onCloseRequest,
    ) { content() }
}

@Composable
private fun EditFeedScreenContent(feedSource: FeedSource, onCloseRequest: () -> Unit, modifier: Modifier = Modifier) {
    val vm = koinViewModel<EditFeedViewModel>()
    val close by rememberUpdatedState(onCloseRequest)
    LaunchedEffect(feedSource) { vm.loadFeedToEdit(feedSource) }
    val url by vm.feedUrlState.collectAsState()
    val name by vm.feedNameState.collectAsState()
    val settings by vm.feedSourceSettingsState.collectAsState()
    val categories by vm.categoriesState.collectAsState()
    val showNotifications by vm.showNotificationToggleState.collectAsState()
    val strings = LocalFeedFlowStrings.current
    var loading by remember { mutableStateOf(false) }
    var completionRevision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.feedEditedState.collect { state ->
            when (state) {
                is FeedEditedState.Error -> {
                    completionRevision++
                    showError = true
                    loading = false
                    error = when (state) {
                        FeedEditedState.Error.InvalidUrl -> strings.invalidRssUrl
                        FeedEditedState.Error.InvalidTitleLink -> strings.missingTitleAndLink
                        FeedEditedState.Error.GenericError -> strings.editFeedGenericError
                        is FeedEditedState.Error.FetchFailed ->
                            strings.feedFetchRefusedError
                    }
                }
                is FeedEditedState.FeedEdited -> {
                    completionRevision++
                    loading = false
                    close()
                }
                FeedEditedState.Idle -> {
                    loading = false
                    showError = false
                    error = ""
                }
                FeedEditedState.Loading -> loading = true
            }
        }
    }
    LaunchedEffect(Unit) {
        vm.feedDeletedState.collect {
            showDelete = false
            close()
        }
    }
    EditFeedForm(
        feedUrl = url, feedName = name, showError = showError, showLoading = loading,
        errorMessage = error, canEditUrl = vm.canEditUrl(), categoriesState = categories,
        feedSourceSettings = settings,
        onFeedUrlUpdated = vm::updateFeedUrlTextFieldValue,
        onFeedNameUpdated = vm::updateFeedNameTextFieldValue,
        articleOpenModeSelector = { m, enabled ->
            ArticleOpenModeSelector(
                settings.articleOpenMode,
                vm::updateArticleOpenMode,
                modifier = m,
                allowDefault = true,
                enabled = enabled,
            )
        },
        onHiddenToggled = vm::updateIsHiddenFromTimeline,
        onHideImagesToggled = vm::updateIsHideImagesEnabled,
        onPinnedToggled = vm::updateIsPinned,
        showNotificationToggle = showNotifications,
        onNotificationToggleChanged = vm::updateIsNotificationEnabled,
        categoryActions = FeedCategoryActions(
            onSelect = vm::onCategorySelected,
            onAdd = vm::addNewCategory,
            validate = vm::validateCategoryName,
        ),
        onCancel = onCloseRequest,
        editFeed = vm::editFeed,
        onShowDeleteDialog = { showDelete = true }, showDeleteDialog = showDelete,
        onDismissDeleteDialog = { showDelete = false }, onConfirmDelete = vm::deleteFeed,
        modifier = modifier,
        completionRevision = completionRevision,
    )
}
