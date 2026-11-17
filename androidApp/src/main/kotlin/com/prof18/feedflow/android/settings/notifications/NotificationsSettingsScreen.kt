package com.prof18.feedflow.android.settings.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.notifications.BackgroundRefreshTurnedOnSnackbar
import com.prof18.feedflow.android.notifications.rememberNotificationPermission
import com.prof18.feedflow.android.settings.SettingsE2eIds
import com.prof18.feedflow.android.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.android.ui.settings.NotificationToggleRow
import com.prof18.feedflow.android.ui.settings.SettingDropdownOption
import com.prof18.feedflow.android.ui.settings.SettingRowDefaults
import com.prof18.feedflow.android.ui.settings.SettingsSectionHeader
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.NotificationSettingState
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.NotificationsViewModel
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun NotificationsSettingsScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<NotificationsViewModel>()
    val notificationState by viewModel.notificationSettingState.collectAsStateWithLifecycle()
    val syncPeriod by viewModel.syncPeriodFlow.collectAsStateWithLifecycle()
    val permission = rememberNotificationPermission()

    val snackbarHostState = remember { SnackbarHostState() }
    BackgroundRefreshTurnedOnSnackbar(viewModel.backgroundRefreshTurnedOn, snackbarHostState)

    NotificationSettingsScreenContent(
        notificationState = notificationState,
        syncPeriod = syncPeriod,
        hasNotificationPermission = permission.isGranted,
        onNavigateBack = navigateBack,
        onSyncPeriodSelected = viewModel::updateSyncPeriod,
        onRequestPermission = permission.request,
        onAllNotificationsToggle = { status ->
            viewModel.updateAllNotificationStatus(status)
        },
        onFeedSourceNotificationsToggle = { feedSourceId, status ->
            viewModel.updateNotificationStatus(status, feedSourceId)
        },
        onNotificationModeSelected = { mode ->
            viewModel.updateNotificationMode(mode)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
}

@Composable
internal fun NotificationSettingsScreenContent(
    notificationState: NotificationSettingState,
    syncPeriod: SyncPeriod,
    hasNotificationPermission: Boolean,
    onNavigateBack: () -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onRequestPermission: () -> Unit,
    onAllNotificationsToggle: (Boolean) -> Unit,
    onFeedSourceNotificationsToggle: (feedSourceId: String, status: Boolean) -> Unit,
    onNotificationModeSelected: (NotificationMode) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current

    Scaffold(
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = {
                    Text(strings.settingsNotificationsTitle)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (notificationState.feedSources.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.regular),
                    text = LocalFeedFlowStrings.current.settingsNotificationNoFeed,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            val layoutDir = LocalLayoutDirection.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                    .padding(end = paddingValues.calculateRightPadding(layoutDir)),
            ) {
                item {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacing.regular)
                            .padding(top = Spacing.xsmall, bottom = Spacing.regular),
                        text = strings.settingsNotificationsWarningReliability,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (!hasNotificationPermission) {
                        NotificationPermissionRow(
                            modifier = Modifier.testTag(SettingsE2eIds.NOTIFICATIONS_PERMISSION_ROW),
                            onRequestPermission = onRequestPermission,
                        )
                    }

                    BackgroundRefreshRow(
                        modifier = Modifier.testTag(SettingsE2eIds.NOTIFICATIONS_BACKGROUND_REFRESH),
                        syncPeriod = syncPeriod,
                        onSyncPeriodSelected = onSyncPeriodSelected,
                    )

                    NotificationModeSelector(
                        currentMode = notificationState.notificationMode,
                        onModeSelected = onNotificationModeSelected,
                    )

                    if (notificationState.notificationMode == NotificationMode.ARTICLE) {
                        Text(
                            text = strings.settingsNotificationModeArticleDesc,
                            modifier = Modifier.padding(horizontal = Spacing.regular),
                            style = SettingRowDefaults.descriptionStyle,
                            color = SettingRowDefaults.descriptionColor,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xsmall))

                    NotificationToggleRow(
                        modifier = Modifier.testTag(SettingsE2eIds.NOTIFICATIONS_ENABLE_ALL),
                        title = strings.settingsNotificationsEnableAllTitle,
                        isChecked = notificationState.isEnabledForAll,
                        onCheckedChange = {
                            onAllNotificationsToggle(!notificationState.isEnabledForAll)
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.regular),
                    )
                }

                notificationState.feedGroups.forEach { group ->
                    item(key = group.categoryTitle?.let { "group_named_$it" } ?: "group_uncategorized") {
                        SettingsSectionHeader(title = group.categoryTitle ?: strings.noCategory)
                    }

                    items(
                        items = group.feedSources,
                        key = { it.feedSourceId },
                    ) { feedSource ->
                        NotificationToggleRow(
                            modifier = Modifier.testTag(
                                SettingsE2eIds.NOTIFICATIONS_FEED_PREFIX + feedSource.feedSourceId,
                            ),
                            title = feedSource.feedSourceTitle,
                            isChecked = feedSource.isEnabled,
                            onCheckedChange = { isEnabled ->
                                onFeedSourceNotificationsToggle(
                                    feedSource.feedSourceId,
                                    isEnabled,
                                )
                            },
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                }
            }
        }
    }
}

@Composable
private fun NotificationModeSelector(
    currentMode: NotificationMode,
    onModeSelected: (NotificationMode) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    CompactSettingDropdownRow(
        title = strings.settingsNotificationMode,
        currentValue = currentMode,
        options = persistentListOf(
            SettingDropdownOption(
                NotificationMode.ARTICLE,
                strings.settingsNotificationModeArticle,
            ),
            SettingDropdownOption(
                NotificationMode.FEED_SOURCE,
                strings.settingsNotificationModeFeedSource,
            ),
            SettingDropdownOption(
                NotificationMode.CATEGORY,
                strings.settingsNotificationModeCategory,
            ),
            SettingDropdownOption(
                NotificationMode.GROUPED,
                strings.settingsNotificationModeGrouped,
            ),
        ),
        onOptionSelected = onModeSelected,
    )
}
