package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.utils.FeatureFlags
import com.prof18.feedflow.core.utils.Websites.FEED_FLOW_WEBSITE
import com.prof18.feedflow.core.utils.Websites.MG_WEBSITE
import com.prof18.feedflow.core.utils.Websites.TRANSLATION_WEBSITE
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.about.LicensesScreen
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.disableSentry
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.desktop.utils.openUriSafely
import com.prof18.feedflow.shared.ui.about.AboutButtonItem
import com.prof18.feedflow.shared.ui.about.AboutTextItem
import com.prof18.feedflow.shared.ui.about.AuthorText
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

@Composable
internal fun AboutPane(
    isCrashReportingEnabled: Boolean,
    onCrashReportingToggled: (Boolean) -> Unit,
    showWindowsRendererSetting: Boolean,
    isWindowsOpenGLRendererEnabled: Boolean,
    onWindowsRendererSelected: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val appConfig = remember { DI.koin.get<DesktopConfig>() }
    val userFeedbackReporter = remember { DI.koin.get<UserFeedbackReporter>() }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLicenses by remember { mutableStateOf(false) }
    fun showExternalOpenError() {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = strings.browserLaunchError,
                duration = SnackbarDuration.Short,
            )
        }
    }

    fun openExternalUrl(url: String) {
        if (!uriHandler.openUriSafely(url)) {
            showExternalOpenError()
        }
    }

    if (showLicenses) {
        LicensesScreen(
            onBackClick = { showLicenses = false },
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (appConfig.appEnvironment.isRelease()) {
                SettingSwitchItem(
                    title = strings.settingsCrashReporting,
                    isChecked = isCrashReportingEnabled,
                    onCheckedChange = onCrashReportingToggled,
                )
            }

            if (showWindowsRendererSetting) {
                CompactSettingDropdownRow(
                    title = "Windows renderer",
                    currentValue = isWindowsOpenGLRendererEnabled,
                    options = persistentListOf(
                        SettingDropdownOption(false, "Default"),
                        SettingDropdownOption(true, "OpenGL (restart required)"),
                    ),
                    onOptionSelected = onWindowsRendererSelected,
                )
            }

            SettingItem(
                title = strings.reportIssueButton,
                onClick = {
                    runCatching {
                        val uri = URI.create(
                            userFeedbackReporter.getEmailUrl(
                                subject = strings.issueContentTitle,
                                content = strings.issueContentTemplate,
                            ),
                        )
                        Desktop.getDesktop().mail(uri)
                    }.onFailure { showExternalOpenError() }
                },
            )

            if (FeatureFlags.ENABLE_FAQ) {
                SettingItem(
                    title = strings.aboutMenuFaq,
                    onClick = {
                        val languageCode = java.util.Locale.getDefault().language
                        openExternalUrl("https://feedflow.dev/$languageCode/faq")
                    },
                )
            }

            AboutTextItem(modifier = Modifier.padding(Spacing.regular))

            AboutButtonItem(
                onClick = { openExternalUrl(FEED_FLOW_WEBSITE) },
                buttonText = strings.openWebsiteButton,
            )

            AboutButtonItem(
                onClick = { openExternalUrl(TRANSLATION_WEBSITE) },
                buttonText = strings.aboutMenuContributeTranslations,
            )

            AboutButtonItem(
                onClick = { showLicenses = true },
                buttonText = strings.openSourceLicenses,
            )

            val versionLabel = appConfig.version?.let { strings.aboutAppVersion(it) } ?: ""
            if (versionLabel.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.regular),
                    text = versionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }

            AuthorText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.medium),
                nameClicked = { openExternalUrl(MG_WEBSITE) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

internal fun updateDesktopCrashReporting(enabled: Boolean) {
    val appConfig = DI.koin.get<DesktopConfig>()

    if (!enabled) {
        disableSentry()
        return
    }

    if (
        appConfig.appEnvironment.isRelease() &&
        appConfig.sentryDns != null &&
        appConfig.version != null
    ) {
        initSentry(
            dns = appConfig.sentryDns,
            version = appConfig.version,
        )
    }
}

@Preview
@Composable
private fun AboutPanePreview() {
    FeedFlowTheme {
        AboutPane(
            isCrashReportingEnabled = false,
            onCrashReportingToggled = {},
            showWindowsRendererSetting = true,
            isWindowsOpenGLRendererEnabled = true,
            onWindowsRendererSelected = {},
        )
    }
}
