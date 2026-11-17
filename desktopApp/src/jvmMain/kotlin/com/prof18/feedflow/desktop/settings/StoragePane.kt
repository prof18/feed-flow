package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.DesktopFeedbackTone
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.InlineFeedbackStatus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun StoragePane(
    autoDeletePeriod: AutoDeletePeriod,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onClearDownloadedArticles: () -> Unit,
    downloadedArticlesCleared: Flow<Boolean>,
    onClearOldArticles: () -> Unit = {},
    canClearOldArticles: Boolean = true,
) {
    val strings = LocalFeedFlowStrings.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val feedbackState = remember { DesktopFeedbackState() }

    LaunchedEffect(downloadedArticlesCleared, feedbackState, strings) {
        downloadedArticlesCleared.collect { cleared ->
            // showFeedback suspends until the message is dismissed; launch so the collector never waits on it.
            launch {
                if (cleared) {
                    feedbackState.showSuccess(strings.settingsOfflineCopiesDeleted)
                } else {
                    feedbackState.showError(strings.settingsOfflineCopiesDeleteFailed)
                }
            }
        }
    }

    SettingsPage(title = strings.settingsStorage) {
        SettingsSection {
            SettingsChoice(
                title = strings.settingsAutoDelete,
                selected = autoDeletePeriod,
                choices = persistentListOf(
                    AutoDeletePeriod.DISABLED to strings.settingsAutoDeletePeriodDisabled,
                    AutoDeletePeriod.ONE_DAY to strings.settingsAutoDeletePeriodOneDay,
                    AutoDeletePeriod.ONE_WEEK to strings.settingsAutoDeletePeriodOneWeek,
                    AutoDeletePeriod.TWO_WEEKS to strings.settingsAutoDeletePeriodTwoWeeks,
                    AutoDeletePeriod.ONE_MONTH to strings.settingsAutoDeletePeriodOneMonth,
                ),
                onSelected = onAutoDeletePeriodSelected,
                supportingText = strings.settingsAutoDeleteDescription,
            )
        }
        SettingsSection(title = strings.settingsClearDataTitle) {
            ConfirmationSetting(
                title = strings.clearOldArticlesButton,
                dialogTitle = strings.clearOldArticlesButton,
                dialogMessage = strings.clearOldArticlesDialogMessage,
                onConfirm = onClearOldArticles,
                enabled = canClearOldArticles,
            )
            ConfirmationSetting(
                title = strings.settingsClearDownloadedArticles,
                supportingText = strings.settingsClearDownloadedArticlesDescription,
                dialogTitle = strings.settingsClearDownloadedArticlesDialogTitle,
                dialogMessage = strings.settingsClearDownloadedArticlesDialogMessage,
                onConfirm = onClearDownloadedArticles,
            )
            ConfirmationSetting(
                title = strings.settingsClearImageCache,
                dialogTitle = strings.settingsClearImageCacheDialogTitle,
                dialogMessage = strings.settingsClearImageCacheDialogMessage,
                onConfirm = {
                    scope.launch {
                        val imageLoader = SingletonImageLoader.get(context)
                        try {
                            imageLoader.memoryCache?.clear()
                            withContext(Dispatchers.IO) { imageLoader.diskCache?.clear() }
                        } catch (_: IOException) {
                            feedbackState.showError(strings.settingsImageCacheClearFailed)
                            return@launch
                        }
                        feedbackState.showSuccess(strings.settingsImageCacheCleared)
                    }
                },
                destructive = false,
            )
        }
        InlineFeedbackStatus(feedbackState, Modifier.padding(start = 4.dp, top = 8.dp))
    }
}

private suspend fun DesktopFeedbackState.showSuccess(message: String) {
    showFeedback(message, tone = DesktopFeedbackTone.Success)
}

private suspend fun DesktopFeedbackState.showError(message: String) {
    showFeedback(message, tone = DesktopFeedbackTone.Error)
}
