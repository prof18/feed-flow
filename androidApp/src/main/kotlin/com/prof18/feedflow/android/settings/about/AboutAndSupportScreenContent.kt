package com.prof18.feedflow.android.settings.about

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.utils.FeatureFlags
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AboutAndSupportScreenContent(
    showCrashReporting: Boolean,
    isCrashReportingEnabled: Boolean,
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
    onCrashReportingEnabled: (Boolean) -> Unit,
    onReportIssueClick: () -> Unit,
    onFaqClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsAboutAndSupport) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.reportIssueButton,
                    icon = Icons.Outlined.BugReport,
                    onClick = onReportIssueClick,
                )
            }

            if (showCrashReporting) {
                item {
                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsCrashReporting,
                        icon = Icons.Outlined.Report,
                        isChecked = isCrashReportingEnabled,
                        onCheckedChange = onCrashReportingEnabled,
                    )
                }
            }

            if (FeatureFlags.ENABLE_FAQ) {
                item {
                    SettingItem(
                        title = LocalFeedFlowStrings.current.aboutMenuFaq,
                        icon = Icons.Outlined.QuestionMark,
                        onClick = onFaqClick,
                    )
                }
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.aboutButton,
                    icon = Icons.Outlined.Info,
                    onClick = onAboutClick,
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Preview
@Composable
private fun AboutAndSupportScreenContentPreview() {
    FeedFlowTheme {
        AboutAndSupportScreenContent(
            showCrashReporting = true,
            isCrashReportingEnabled = true,
            navigateBack = {},
            onAboutClick = {},
            onCrashReportingEnabled = {},
            onReportIssueClick = {},
            onFaqClick = {},
        )
    }
}
