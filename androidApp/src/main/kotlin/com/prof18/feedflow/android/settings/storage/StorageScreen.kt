package com.prof18.feedflow.android.settings.storage

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.imageLoader
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.ReadingBehaviorSettingsViewModel
import com.prof18.feedflow.shared.presentation.SyncAndStorageSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import java.io.IOException

@Composable
internal fun StorageScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<SyncAndStorageSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val readingViewModel = koinViewModel<ReadingBehaviorSettingsViewModel>()
    val readingState by readingViewModel.state.collectAsStateWithLifecycle()
    val strings = LocalFeedFlowStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState, strings) {
        viewModel.downloadedArticlesCleared.collect { cleared ->
            val message = if (cleared) {
                strings.settingsOfflineCopiesDeleted
            } else {
                strings.settingsOfflineCopiesDeleteFailed
            }
            // showSnackbar suspends until dismissed; launch so the view model's emit never waits on it.
            launch { snackbarHostState.showShortSnackbar(message) }
        }
    }

    StorageScreenContent(
        navigateBack = navigateBack,
        autoDeletePeriod = state.autoDeletePeriod,
        onAutoDeletePeriodSelected = { period ->
            viewModel.updateAutoDeletePeriod(period)
        },
        isSaveReaderModeContentEnabled = readingState.isSaveReaderModeContentEnabled,
        isPrefetchArticleContentEnabled = readingState.isPrefetchArticleContentEnabled,
        onSaveReaderModeContentChange = readingViewModel::updateSaveReaderModeContent,
        onPrefetchArticleContentChange = readingViewModel::updatePrefetchArticleContent,
        onClearDownloadedArticles = {
            viewModel.clearDownloadedArticleContent()
        },
        onClearImageCache = {
            scope.launch {
                val imageLoader = context.applicationContext.imageLoader
                val message = try {
                    imageLoader.memoryCache?.clear()
                    withContext(Dispatchers.IO) { imageLoader.diskCache?.clear() }
                    strings.settingsImageCacheCleared
                } catch (_: IOException) {
                    strings.settingsImageCacheClearFailed
                }
                snackbarHostState.showShortSnackbar(message)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
}

private suspend fun SnackbarHostState.showShortSnackbar(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message, duration = SnackbarDuration.Short)
}
