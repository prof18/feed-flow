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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MarkAsUnread
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.MR
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.settings.components.BrowserSelectionDialog
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.domain.model.Browser
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import com.prof18.feedflow.shared.ui.preview.FeedFlowPhonePreview
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    navigateToImportExport: () -> Unit,
) {
    val context = LocalContext.current

    val browserManager = koinInject<BrowserManager>()
    val settingsViewModel = koinViewModel<SettingsViewModel>()

    val browserListState by browserManager.browserListState.collectAsStateWithLifecycle()
    val settingState by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    val emailSubject = stringResource(MR.strings.issue_content_title)
    val emailContent = stringResource(MR.strings.issue_content_template)
    val chooserTitle = stringResource(MR.strings.issue_report_title)

    SettingsScreenContent(
        browsers = browserListState,
        onFeedListClick = onFeedListClick,
        onAddFeedClick = onAddFeedClick,
        isMarkReadWhenScrollingEnabled = settingState.isMarkReadWhenScrollingEnabled,
        isShowReadItemEnabled = settingState.isShowReadItemsEnabled,
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
        setMarkReadWhenScrolling = { enabled ->
            settingsViewModel.updateMarkReadWhenScrolling(enabled)
        },
        setShowReadItem = { enabled ->
            settingsViewModel.updateShowReadItemsOnTimeline(enabled)
        },
    )
}

@Composable
private fun SettingsScreenContent(
    browsers: List<Browser>,
    isMarkReadWhenScrollingEnabled: Boolean,
    isShowReadItemEnabled: Boolean,
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    onBrowserSelected: (Browser) -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
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
            isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
            setMarkReadWhenScrolling = setMarkReadWhenScrolling,
            isShowReadItemEnabled = isShowReadItemEnabled,
            setShowReadItem = setShowReadItem,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SettingsList(
    isMarkReadWhenScrollingEnabled: Boolean,
    isShowReadItemEnabled: Boolean,
    modifier: Modifier = Modifier,
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    onBrowserSelectionClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Text(
                text = stringResource(resource = MR.strings.settings_general_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(Spacing.regular),
            )
        }

        item {
            SettingItem(
                modifier = Modifier
                    .tagForTesting(TestingTag.SETTINGS_FEED_ITEM),
                title = stringResource(resource = MR.strings.feeds_title),
                icon = Icons.Default.Feed,
                onClick = onFeedListClick,
            )
        }

        item {
            SettingItem(
                title = stringResource(resource = MR.strings.add_feed),
                icon = Icons.Outlined.AddCircleOutline,
                onClick = onAddFeedClick,
            )
        }

        item {
            SettingItem(
                title = stringResource(resource = MR.strings.import_export_opml),
                icon = Icons.Outlined.SwapVert,
                onClick = navigateToImportExport,
            )
        }

        item {
            SettingItem(
                modifier = Modifier
                    .tagForTesting(TestingTag.BROWSER_SELECTOR),
                title = stringResource(resource = MR.strings.browser_selection_button),
                icon = Icons.Outlined.Language,
                onClick = onBrowserSelectionClick,
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
            Text(
                text = stringResource(resource = MR.strings.settings_app_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(Spacing.regular),
            )
        }

        item {
            SettingItem(
                title = stringResource(resource = MR.strings.report_issue_button),
                icon = Icons.Outlined.BugReport,
                onClick = onBugReportClick,
            )
        }

        item {
            SettingItem(
                modifier = Modifier
                    .tagForTesting(TestingTag.ABOUT_SETTINGS_ITEM),
                title = stringResource(resource = MR.strings.about_button),
                icon = Icons.Outlined.Info,
                onClick = onAboutClick,
            )
        }
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
            text = stringResource(resource = MR.strings.toggle_mark_read_when_scrolling),
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
            Icons.Outlined.PlaylistAddCheck,
            contentDescription = null,
        )

        Text(
            text = stringResource(resource = MR.strings.settings_toggle_show_read_articles),
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
private fun SettingsNavBar(navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                stringResource(resource = MR.strings.settings_title),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navigateBack()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}

@FeedFlowPhonePreview
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        SettingsScreenContent(
            browsers = browsersForPreview,
            isMarkReadWhenScrollingEnabled = true,
            isShowReadItemEnabled = false,
            onFeedListClick = {},
            onAddFeedClick = {},
            onBrowserSelected = {},
            navigateBack = {},
            onAboutClick = {},
            onBugReportClick = {},
            navigateToImportExport = {},
            setMarkReadWhenScrolling = {},
            setShowReadItem = {},
        )
    }
}
