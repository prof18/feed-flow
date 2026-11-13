package com.prof18.feedflow.android.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.feedsourcelist.FeedSourceListE2eIds
import com.prof18.feedflow.android.ui.preview.PreviewPhone
import com.prof18.feedflow.android.ui.settings.SettingItem
import com.prof18.feedflow.android.ui.settings.SettingsSectionHeader
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings

@Composable
internal fun SettingsScreenContent(
    appVersion: String,
    navigateBack: () -> Unit,
    navigateToFeeds: () -> Unit,
    navigateToAddFeed: () -> Unit,
    navigateToImportExport: () -> Unit,
    navigateToBlockedWords: () -> Unit,
    navigateToSyncAndRefresh: () -> Unit,
    navigateToNotifications: () -> Unit,
    navigateToStorage: () -> Unit,
    navigateToAppearance: () -> Unit,
    navigateToFeedListSettings: () -> Unit,
    navigateToReadingBehavior: () -> Unit,
    navigateToWidgetSettings: () -> Unit,
    navigateToAboutAndSupport: () -> Unit,
    onSupportProjectClick: () -> Unit,
    showWidgetSettings: Boolean,
    showSupportProject: Boolean,
) {
    Scaffold(
        topBar = {
            SettingsNavBar(navigateBack)
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .testTag(SettingsE2eIds.ROOT_LIST)
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
                SettingsSectionHeader(title = LocalFeedFlowStrings.current.feedsTitle)
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(FeedSourceListE2eIds.SETTINGS_ROW),
                    title = LocalFeedFlowStrings.current.feedsTitle,
                    icon = Icons.AutoMirrored.Default.Feed,
                    onClick = navigateToFeeds,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.addFeed,
                    icon = Icons.Outlined.AddCircleOutline,
                    onClick = navigateToAddFeed,
                )
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.IMPORT_EXPORT_ROW),
                    title = LocalFeedFlowStrings.current.importExportLabel,
                    icon = Icons.Outlined.SwapVert,
                    onClick = navigateToImportExport,
                )
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.BLOCKED_WORDS_ROW),
                    title = LocalFeedFlowStrings.current.settingsBlockedWords,
                    icon = Icons.Outlined.Report,
                    onClick = navigateToBlockedWords,
                )
            }

            item {
                SettingsSectionHeader(title = LocalFeedFlowStrings.current.settingsSyncAndStorage)
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.SYNC_ROW),
                    title = LocalFeedFlowStrings.current.settingsSyncAndRefresh,
                    supportingText = LocalFeedFlowStrings.current.settingsSyncAndRefreshSubtitle,
                    icon = Icons.Outlined.Sync,
                    onClick = navigateToSyncAndRefresh,
                )
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.NOTIFICATIONS_ROW),
                    title = LocalFeedFlowStrings.current.settingsNotificationsTitle,
                    supportingText = LocalFeedFlowStrings.current.settingsNotificationsSubtitle,
                    icon = Icons.Outlined.Notifications,
                    onClick = navigateToNotifications,
                )
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.STORAGE_ROW),
                    title = LocalFeedFlowStrings.current.settingsStorage,
                    supportingText = LocalFeedFlowStrings.current.settingsStorageSubtitle,
                    icon = Icons.Outlined.Storage,
                    onClick = navigateToStorage,
                )
            }

            item {
                SettingsSectionHeader(title = LocalFeedFlowStrings.current.settingsSectionDisplayAndReading)
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.APPEARANCE_ROW),
                    title = LocalFeedFlowStrings.current.settingsAppearance,
                    supportingText = LocalFeedFlowStrings.current.settingsAppearanceSubtitle,
                    icon = Icons.Outlined.Palette,
                    onClick = navigateToAppearance,
                )
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.FEED_LIST_ROW),
                    title = LocalFeedFlowStrings.current.settingsFeedListTitle,
                    supportingText = LocalFeedFlowStrings.current.settingsFeedListSubtitle,
                    icon = Icons.Outlined.Layers,
                    onClick = navigateToFeedListSettings,
                )
            }

            item {
                SettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.READING_BEHAVIOR_ROW),
                    title = LocalFeedFlowStrings.current.settingsReadingBehavior,
                    supportingText = LocalFeedFlowStrings.current.settingsReadingBehaviorSubtitle,
                    icon = Icons.Outlined.LocalLibrary,
                    onClick = navigateToReadingBehavior,
                )
            }

            if (showWidgetSettings) {
                item {
                    SettingItem(
                        title = LocalFeedFlowStrings.current.widgetConfigurationTitle,
                        supportingText = LocalFeedFlowStrings.current.settingsWidgetSubtitle,
                        icon = Icons.Outlined.Widgets,
                        onClick = navigateToWidgetSettings,
                    )
                }
            }

            if (showSupportProject) {
                item {
                    SettingItem(
                        modifier = Modifier.testTag(SettingsE2eIds.SUPPORT_PROJECT_ROW),
                        title = LocalFeedFlowStrings.current.supportTheProject,
                        icon = Icons.Outlined.FavoriteBorder,
                        onClick = onSupportProjectClick,
                    )
                }
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsAboutAndSupport,
                    icon = Icons.Outlined.Info,
                    onClick = navigateToAboutAndSupport,
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.aboutAppVersion(appVersion),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun SettingsNavBar(navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(LocalFeedFlowStrings.current.settingsTitle)
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(SettingsE2eIds.BACK_BUTTON),
                onClick = {
                    navigateBack()
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}

@PreviewPhone
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        SettingsScreenContent(
            appVersion = "1.0.0",
            navigateBack = {},
            navigateToFeeds = {},
            navigateToAddFeed = {},
            navigateToImportExport = {},
            navigateToBlockedWords = {},
            navigateToSyncAndRefresh = {},
            navigateToNotifications = {},
            navigateToStorage = {},
            navigateToAppearance = {},
            navigateToFeedListSettings = {},
            navigateToReadingBehavior = {},
            navigateToWidgetSettings = {},
            navigateToAboutAndSupport = {},
            onSupportProjectClick = {},
            showWidgetSettings = true,
            showSupportProject = true,
        )
    }
}
