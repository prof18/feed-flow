package com.prof18.feedflow.android.settings

import FeedFlowTheme
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MarkAsUnread
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.settings.components.BrowserSelectionDialog
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.domain.model.Browser
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.collections.immutable.ImmutableList
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    navigateToAccounts: () -> Unit,
) {
    val context = LocalContext.current

    val browserManager = koinInject<BrowserManager>()
    val settingsViewModel = koinViewModel<SettingsViewModel>()

    val browserListState by browserManager.browserListState.collectAsStateWithLifecycle()
    val settingState by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
    val emailContent = LocalFeedFlowStrings.current.issueContentTemplate
    val chooserTitle = LocalFeedFlowStrings.current.issueReportTitle

    SettingsScreenContent(
        browsers = browserListState,
        onFeedListClick = onFeedListClick,
        onAddFeedClick = onAddFeedClick,
        isMarkReadWhenScrollingEnabled = settingState.isMarkReadWhenScrollingEnabled,
        isShowReadItemEnabled = settingState.isShowReadItemsEnabled,
        isReaderModeEnabled = settingState.isReaderModeEnabled,
        isRemoveTitleFromDescriptionEnabled = settingState.isRemoveTitleFromDescriptionEnabled,
        onBrowserSelected = { browser ->
            browserManager.setFavouriteBrowser(browser)
        },
        navigateBack = navigateBack,
        onAboutClick = onAboutClick,
        onBugReportClick = {
            val uri = Uri.parse(
                UserFeedbackReporter.getEmailUrl(
                    subject = emailSubject,
                    content = emailContent,
                ),
            )
            val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
            context.startActivity(Intent.createChooser(emailIntent, chooserTitle))
        },
        navigateToImportExport = navigateToImportExport,
        navigateToAccounts = navigateToAccounts,
        setMarkReadWhenScrolling = { enabled ->
            settingsViewModel.updateMarkReadWhenScrolling(enabled)
        },
        setShowReadItem = { enabled ->
            settingsViewModel.updateShowReadItemsOnTimeline(enabled)
        },
        setReaderMode = { enabled ->
            settingsViewModel.updateReaderMode(enabled)
        },
        setRemoveTitleFromDescription = { enabled ->
            settingsViewModel.updateRemoveTitleFromDescription(enabled)
        },
    )
}

@Composable
private fun SettingsScreenContent(
    browsers: ImmutableList<Browser>,
    isMarkReadWhenScrollingEnabled: Boolean,
    isShowReadItemEnabled: Boolean,
    isReaderModeEnabled: Boolean,
    isRemoveTitleFromDescriptionEnabled: Boolean,
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    onBrowserSelected: (Browser) -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    navigateToAccounts: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
    setReaderMode: (Boolean) -> Unit,
    setRemoveTitleFromDescription: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsNavBar(navigateBack)
        },
    ) { paddingValues ->

        var showBrowserSelection by remember {
            mutableStateOf(
                false,
            )
        }

        if (showBrowserSelection) {
            BrowserSelectionDialog(
                browserList = browsers,
                onBrowserSelected = { browser ->
                    onBrowserSelected(browser)
                },
                dismissDialog = {
                    showBrowserSelection = false
                },
            )
        }

        SettingsList(
            modifier = Modifier
                .padding(paddingValues),
            onFeedListClick = onFeedListClick,
            onAddFeedClick = onAddFeedClick,
            onBrowserSelectionClick = {
                showBrowserSelection = true
            },
            navigateToImportExport = navigateToImportExport,
            onAboutClick = onAboutClick,
            onBugReportClick = onBugReportClick,
            navigateToAccounts = navigateToAccounts,
            isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
            setMarkReadWhenScrolling = setMarkReadWhenScrolling,
            isShowReadItemEnabled = isShowReadItemEnabled,
            setShowReadItem = setShowReadItem,
            isReaderModeEnabled = isReaderModeEnabled,
            setReaderMode = setReaderMode,
            isRemoveTitleFromDescriptionEnabled = isRemoveTitleFromDescriptionEnabled,
            setRemoveTitleFromDescription = setRemoveTitleFromDescription,
        )
    }
}

@Composable
private fun SettingsList(
    isMarkReadWhenScrollingEnabled: Boolean,
    isShowReadItemEnabled: Boolean,
    isReaderModeEnabled: Boolean,
    isRemoveTitleFromDescriptionEnabled: Boolean,
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    onBrowserSelectionClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    navigateToAccounts: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
    setReaderMode: (Boolean) -> Unit,
    setRemoveTitleFromDescription: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Text(
                text = LocalFeedFlowStrings.current.settingsTitleFeed,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(Spacing.regular),
            )
        }

        item {
            SettingItem(
                modifier = Modifier
                    .tagForTesting(TestingTag.SETTINGS_FEED_ITEM),
                title = LocalFeedFlowStrings.current.feedsTitle,
                icon = Icons.AutoMirrored.Default.Feed,
                onClick = onFeedListClick,
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.addFeed,
                icon = Icons.Outlined.AddCircleOutline,
                onClick = onAddFeedClick,
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.importExportOpml,
                icon = Icons.Outlined.SwapVert,
                onClick = navigateToImportExport,
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.settingsAccounts,
                icon = Icons.Outlined.Sync,
                onClick = navigateToAccounts,
            )
        }

        item {
            Text(
                text = LocalFeedFlowStrings.current.settingsBehaviourTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(Spacing.regular),
            )
        }

        item {
            SettingItem(
                modifier = Modifier
                    .tagForTesting(TestingTag.BROWSER_SELECTOR),
                title = LocalFeedFlowStrings.current.browserSelectionButton,
                icon = Icons.Outlined.Language,
                onClick = onBrowserSelectionClick,
            )
        }

        item {
            ReaderModeSwitch(
                setReaderMode = setReaderMode,
                isReaderModeEnabled = isReaderModeEnabled,
            )
        }

        item {
            MarkReadWhenScrollingSwitch(
                setMarkReadWhenScrolling = setMarkReadWhenScrolling,
                isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
            )
        }

        item {
            ShowReadItemOnTimelineSwitch(
                isShowReadItemEnabled = isShowReadItemEnabled,
                setShowReadItem = setShowReadItem,
            )
        }

        item {
            RemoveTitleFromDescSwitch(
                isRemoveTitleFromDescriptionEnabled = isRemoveTitleFromDescriptionEnabled,
                setRemoveTitleFromDescription = setRemoveTitleFromDescription,
            )
        }

        item {
            Text(
                text = LocalFeedFlowStrings.current.settingsAppTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(Spacing.regular),
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.reportIssueButton,
                icon = Icons.Outlined.BugReport,
                onClick = onBugReportClick,
            )
        }

        item {
            SettingItem(
                modifier = Modifier
                    .tagForTesting(TestingTag.ABOUT_SETTINGS_ITEM),
                title = LocalFeedFlowStrings.current.aboutButton,
                icon = Icons.Outlined.Info,
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
private fun ReaderModeSwitch(
    setReaderMode: (Boolean) -> Unit,
    isReaderModeEnabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setReaderMode(!isReaderModeEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Article,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsReaderMode,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isReaderModeEnabled,
            onCheckedChange = setReaderMode,
        )
    }
}

@Composable
private fun MarkReadWhenScrollingSwitch(
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    isMarkReadWhenScrollingEnabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setMarkReadWhenScrolling(!isMarkReadWhenScrollingEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular)
            .tagForTesting(TestingTag.MARK_AS_READ_SCROLLING_SWITCH),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.MarkAsUnread,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isMarkReadWhenScrollingEnabled,
            onCheckedChange = setMarkReadWhenScrolling,
        )
    }
}

@Composable
private fun ShowReadItemOnTimelineSwitch(
    setShowReadItem: (Boolean) -> Unit,
    isShowReadItemEnabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setShowReadItem(!isShowReadItemEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.PlaylistAddCheck,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isShowReadItemEnabled,
            onCheckedChange = setShowReadItem,
        )
    }
}

@Composable
private fun RemoveTitleFromDescSwitch(
    isRemoveTitleFromDescriptionEnabled: Boolean,
    setRemoveTitleFromDescription: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                setRemoveTitleFromDescription(!isRemoveTitleFromDescriptionEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.HideSource,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsHideDuplicatedTitleFromDesc,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isRemoveTitleFromDescriptionEnabled,
            onCheckedChange = setRemoveTitleFromDescription,
        )
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
                modifier = Modifier
                    .testTag(TestingTag.BACK_BUTTON_FEED_SETTINGS),
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
            browsers = browsersForPreview,
            isMarkReadWhenScrollingEnabled = true,
            isShowReadItemEnabled = false,
            isReaderModeEnabled = false,
            isRemoveTitleFromDescriptionEnabled = false,
            onFeedListClick = {},
            onAddFeedClick = {},
            onBrowserSelected = {},
            navigateBack = {},
            onAboutClick = {},
            onBugReportClick = {},
            navigateToImportExport = {},
            navigateToAccounts = {},
            setMarkReadWhenScrolling = {},
            setShowReadItem = {},
            setReaderMode = {},
            setRemoveTitleFromDescription = {},
        )
    }
}
