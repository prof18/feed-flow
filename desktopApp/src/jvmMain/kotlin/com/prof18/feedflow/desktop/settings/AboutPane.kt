package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.utils.FeatureFlags
import com.prof18.feedflow.core.utils.Websites.FEED_FLOW_WEBSITE
import com.prof18.feedflow.core.utils.Websites.MG_WEBSITE
import com.prof18.feedflow.core.utils.Websites.TRANSLATION_WEBSITE
import com.prof18.feedflow.desktop.DesktopConfig
import com.prof18.feedflow.desktop.about.LicensesScreen
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.utils.disableSentry
import com.prof18.feedflow.desktop.utils.initSentry
import com.prof18.feedflow.desktop.utils.openDesktopMailSafely
import com.prof18.feedflow.desktop.utils.openUriSafely
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun AboutPane(
    isCrashReportingEnabled: Boolean,
    onCrashReportingToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val appConfig = remember { DI.koin.get<DesktopConfig>() }
    val userFeedbackReporter = remember { DI.koin.get<UserFeedbackReporter>() }
    val uriHandler = LocalUriHandler.current
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showOpenError by remember { mutableStateOf(false) }
    var restoreLicensesFocus by remember { mutableStateOf(false) }
    val licensesFocus = remember { FocusRequester() }

    LaunchedEffect(showLicenses, restoreLicensesFocus) {
        if (!showLicenses && restoreLicensesFocus) {
            licensesFocus.requestFocus()
            restoreLicensesFocus = false
        }
    }

    fun openExternalUrl(url: String) {
        showOpenError = !uriHandler.openUriSafely(url)
    }

    if (showLicenses) {
        LicensesScreen(onBackClick = {
            showLicenses = false
            restoreLicensesFocus = true
        })
    } else {
        SettingsPage(title = strings.settingsAboutAndSupport) {
            if (showOpenError) {
                Text(strings.browserLaunchError, color = JewelTheme.globalColors.text.error)
            }
            SettingsSection(title = strings.settingsHelpTitle) {
                SettingsActionRow(
                    title = strings.reportIssueButton,
                    onClick = {
                        val opened = openDesktopMailSafely(
                            userFeedbackReporter.getEmailUrl(
                                subject = strings.issueContentTitle,
                                content = strings.issueContentTemplate,
                            ),
                        )
                        if (!opened) showOpenError = true
                    },
                    trailing = { ExternalLinkIcon() },
                )
                if (FeatureFlags.ENABLE_FAQ) {
                    SettingsActionRow(
                        title = strings.aboutMenuFaq,
                        onClick = {
                            val languageCode = java.util.Locale.getDefault().language
                            openExternalUrl("https://feedflow.dev/$languageCode/faq")
                        },
                        trailing = { ExternalLinkIcon() },
                    )
                }
                SettingsActionRow(
                    title = strings.openWebsiteButton,
                    onClick = { openExternalUrl(FEED_FLOW_WEBSITE) },
                    trailing = { ExternalLinkIcon() },
                )
                SettingsActionRow(
                    title = strings.aboutMenuContributeTranslations,
                    onClick = { openExternalUrl(TRANSLATION_WEBSITE) },
                    trailing = { ExternalLinkIcon() },
                )
            }
            if (appConfig.appEnvironment.isRelease()) {
                SettingsSection {
                    SettingsSwitch(
                        title = strings.settingsCrashReporting,
                        checked = isCrashReportingEnabled,
                        onCheckedChange = onCrashReportingToggled,
                    )
                }
            }
            SettingsSection {
                SettingsBlock {
                    Text(strings.aboutTheApp)
                }
                appConfig.version?.let { version ->
                    SettingsRow(title = strings.aboutAppVersion(version))
                }
                SettingsActionRow(
                    title = "${strings.authorLabel} Marco Gomiero",
                    onClick = { openExternalUrl(MG_WEBSITE) },
                    trailing = { ExternalLinkIcon() },
                )
                SettingsActionRow(
                    title = strings.openSourceLicenses,
                    onClick = { showLicenses = true },
                    showChevron = true,
                    modifier = Modifier.focusRequester(licensesFocus),
                )
            }
        }
    }
}

@Composable
private fun ExternalLinkIcon() {
    Icon(
        Icons.AutoMirrored.Outlined.OpenInNew,
        contentDescription = null,
        tint = JewelTheme.globalColors.text.info,
        modifier = Modifier.size(15.dp),
    )
}

internal fun updateDesktopCrashReporting(enabled: Boolean) {
    val appConfig = DI.koin.get<DesktopConfig>()
    if (!enabled) {
        disableSentry()
        return
    }
    if (appConfig.appEnvironment.isRelease() && appConfig.sentryDns != null && appConfig.version != null) {
        initSentry(dns = appConfig.sentryDns, version = appConfig.version)
    }
}
