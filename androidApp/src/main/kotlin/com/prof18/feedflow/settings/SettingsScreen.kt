@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow.settings

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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.BrowserManager
import com.prof18.feedflow.MR
import com.prof18.feedflow.domain.model.Browser
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.presentation.preview.browsersForPreview
import com.prof18.feedflow.settings.components.BrowserSelectionDialog
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.settings.SettingsDivider
import com.prof18.feedflow.ui.settings.SettingsMenuItem
import com.prof18.feedflow.utils.UserFeedbackReporter
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onFeedListClick: () -> Unit,
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
        isMarkReadWhenScrollingEnabled = settingState.isMarkReadWhenScrollingEnabled,
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
    )
}

@Composable
private fun SettingsScreenContent(
    browsers: List<Browser>,
    isMarkReadWhenScrollingEnabled: Boolean,
    onFeedListClick: () -> Unit,
    onBrowserSelected: (Browser) -> Unit,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
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
            onBrowserSelectionClick = {
                showBrowserSelection = true
            },
            navigateToImportExport = navigateToImportExport,
            onAboutClick = onAboutClick,
            onBugReportClick = onBugReportClick,
            isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
            setMarkReadWhenScrolling = setMarkReadWhenScrolling,

        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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

@Suppress("LongMethod")
@Composable
private fun SettingsList(
    isMarkReadWhenScrollingEnabled: Boolean,
    modifier: Modifier = Modifier,
    onFeedListClick: () -> Unit,
    onBrowserSelectionClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    onAboutClick: () -> Unit,
    onBugReportClick: () -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            SettingsMenuItem(
                text = stringResource(resource = MR.strings.feeds_title),
            ) {
                onFeedListClick()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(resource = MR.strings.browser_selection_button),
            ) {
                onBrowserSelectionClick()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(resource = MR.strings.import_export_opml),
            ) {
                navigateToImportExport()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            MarkReadWhenScrollingSwitch(
                setMarkReadWhenScrolling = setMarkReadWhenScrolling,
                isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
            )
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(
                    resource = MR.strings.report_issue_button,
                ),
            ) {
                onBugReportClick()
            }
        }

        item {
            SettingsDivider()
        }

        item {
            SettingsMenuItem(
                text = stringResource(
                    resource = MR.strings.about_button,
                ),
            ) {
                onAboutClick()
            }
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
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = {
                    setMarkReadWhenScrolling(!isMarkReadWhenScrollingEnabled)
                },
            )
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        Text(
            text = stringResource(resource = MR.strings.toggle_mark_read_when_scrolling),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isMarkReadWhenScrollingEnabled,
            onCheckedChange = setMarkReadWhenScrolling,
        )
    }
}

@FeedFlowPreview
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        SettingsScreenContent(
            browsers = browsersForPreview,
            isMarkReadWhenScrollingEnabled = true,
            onFeedListClick = {},
            onBrowserSelected = {},
            navigateBack = {},
            onAboutClick = {},
            onBugReportClick = {},
            navigateToImportExport = {},
            setMarkReadWhenScrolling = {},
        )
    }
}
