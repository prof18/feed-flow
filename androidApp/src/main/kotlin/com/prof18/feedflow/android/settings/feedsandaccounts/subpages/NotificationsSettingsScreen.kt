package com.prof18.feedflow.android.settings.feedsandaccounts.subpages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BuildConfig
import com.prof18.feedflow.android.settings.components.SyncPeriodDialog
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.NotificationSettingState
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.NotificationsViewModel
import com.prof18.feedflow.shared.ui.settings.NotificationToggleRow
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun NotificationsSettingsScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<NotificationsViewModel>()
    val feedDownloadWorkerEnqueuer = koinInject<FeedDownloadWorkerEnqueuer>()

    val notificationState by viewModel.notificationSettingState.collectAsStateWithLifecycle()
    val syncPeriodState by viewModel.syncPeriodFlow.collectAsStateWithLifecycle()

    NotificationSettingsScreenContent(
        notificationState,
        syncPeriodState,
        onNavigateBack = navigateBack,
        onSyncPeriodSelected = { period ->
            viewModel.updateSyncPeriod(period)
            feedDownloadWorkerEnqueuer.updateWorker(period)
        },
        onAllNotificationsToggle = { status ->
            viewModel.updateAllNotificationStatus(status)
        },
        onFeedSourceNotificationsToggle = { feedSourceId, status ->
            viewModel.updateNotificationStatus(status, feedSourceId)
        },
        onNotificationModeSelected = { mode ->
            viewModel.updateNotificationMode(mode)
        },
        onTriggerManualSync = {
            feedDownloadWorkerEnqueuer.triggerManualSync()
        },
    )
}

@Composable
private fun NotificationSettingsScreenContent(
    notificationState: NotificationSettingState,
    syncPeriodState: SyncPeriod,
    onNavigateBack: () -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onAllNotificationsToggle: (Boolean) -> Unit,
    onFeedSourceNotificationsToggle: (feedSourceId: String, status: Boolean) -> Unit,
    onNotificationModeSelected: (NotificationMode) -> Unit,
    onTriggerManualSync: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    val context = LocalContext.current
    val activity = LocalActivity.current
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var shouldShowRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) {
                shouldShowRationale = shouldShowPermissionDialog(activity)
            }
        },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission = checkNotificationPermission(context)
        shouldShowRationale = shouldShowPermissionDialog(activity)
    }

    Scaffold(
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                    .padding(end = paddingValues.calculateRightPadding(layoutDir)),
            ) {
                Column {
                    if (!hasNotificationPermission) {
                        PermissionStatusRow(
                            hasPermission = hasNotificationPermission,
                            onRequestClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (shouldShowRationale) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        openAppSettings(context)
                                    }
                                } else {
                                    hasNotificationPermission = true
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(Spacing.xsmall))
                    }

                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacing.regular)
                            .padding(bottom = Spacing.regular),
                        text = strings.settingsNotificationsWarningReliability,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    SyncPeriodSelector(
                        currentPeriod = syncPeriodState,
                        onPeriodSelected = onSyncPeriodSelected,
                    )

                    if (BuildConfig.DEBUG) {
                        Button(
                            onClick = onTriggerManualSync,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.regular)
                                .padding(vertical = Spacing.xsmall),
                        ) {
                            Text("Test Notifications Now (Debug)")
                        }
                    }

                    NotificationModeSelector(
                        currentMode = notificationState.notificationMode,
                        onModeSelected = onNotificationModeSelected,
                    )

                    Spacer(modifier = Modifier.height(Spacing.xsmall))

                    NotificationToggleRow(
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

                LazyColumn {
                    items(notificationState.feedSources) { feedSource ->
                        NotificationToggleRow(
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

                    item {
                        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncPeriodSelector(
    currentPeriod: SyncPeriod,
    onPeriodSelected: (SyncPeriod) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.Sync,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = strings.settingsNotificationCheckPeriod,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentPeriod) {
                    SyncPeriod.NEVER -> strings.settingsSyncPeriodNever
                    SyncPeriod.FIFTEEN_MINUTES -> strings.settingsSyncPeriodFifteenMinutes
                    SyncPeriod.THIRTY_MINUTES -> strings.settingsSyncPeriodThirtyMinutes
                    SyncPeriod.ONE_HOUR -> strings.settingsSyncPeriodOneHour
                    SyncPeriod.TWO_HOURS -> strings.settingsSyncPeriodTwoHours
                    SyncPeriod.SIX_HOURS -> strings.settingsSyncPeriodSixHours
                    SyncPeriod.TWELVE_HOURS -> strings.settingsSyncPeriodTwelveHours
                    SyncPeriod.ONE_DAY -> strings.settingsSyncPeriodOneDay
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        SyncPeriodDialog(
            description = LocalFeedFlowStrings.current.settingsNotificationCheckPeriodDesc,
            currentPeriod = currentPeriod,
            onPeriodSelected = onPeriodSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun PermissionStatusRow(
    hasPermission: Boolean,
    onRequestClick: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.regular),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = strings.settingsNotificationsPermissionStatusTitle,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (hasPermission) {
                    strings.settingsNotificationsPermissionStatusGranted
                } else {
                    strings.settingsNotificationsPermissionStatusDenied
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }

        if (!hasPermission) {
            Button(onClick = onRequestClick) {
                Text(strings.settingsNotificationsPermissionRequestButton)
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun shouldShowPermissionDialog(activity: Activity?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ?: false
    } else {
        false
    }
}

private fun openAppSettings(context: Context) {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        context.startActivity(this)
    }
}

@Composable
private fun NotificationModeSelector(
    currentMode: NotificationMode,
    onModeSelected: (NotificationMode) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = strings.settingsNotificationMode,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentMode) {
                    NotificationMode.FEED_SOURCE -> strings.settingsNotificationModeFeedSource
                    NotificationMode.CATEGORY -> strings.settingsNotificationModeCategory
                    NotificationMode.GROUPED -> strings.settingsNotificationModeGrouped
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        NotificationModeDialog(
            currentMode = currentMode,
            onModeSelected = onModeSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun NotificationModeDialog(
    currentMode: NotificationMode,
    onModeSelected: (NotificationMode) -> Unit,
    dismissDialog: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    AlertDialog(
        onDismissRequest = dismissDialog,
        title = {
            Text(text = strings.settingsNotificationMode)
        },
        text = {
            Column {
                Text(
                    text = strings.settingsNotificationModeDesc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.regular),
                )

                NotificationMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onModeSelected(mode)
                                dismissDialog()
                            }
                            .padding(vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = when (mode) {
                                NotificationMode.FEED_SOURCE -> strings.settingsNotificationModeFeedSource
                                NotificationMode.CATEGORY -> strings.settingsNotificationModeCategory
                                NotificationMode.GROUPED -> strings.settingsNotificationModeGrouped
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}
