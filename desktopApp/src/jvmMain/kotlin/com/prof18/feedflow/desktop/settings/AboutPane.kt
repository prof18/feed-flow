package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Link
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
    var showLicenses by remember { mutableStateOf(false) }
    var showOpenError by remember { mutableStateOf(false) }
    val licensesFocus = remember { FocusRequester() }

    fun openExternalUrl(url: String) {
        showOpenError = !uriHandler.openUriSafely(url)
    }

    if (showLicenses) {
        LicensesScreen(onBackClick = {
            showLicenses = false
            licensesFocus.requestFocus()
        })
    }

    SettingsScrollPane(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SettingsGroup(strings.settingsAboutAndSupport)
        if (showOpenError) Text(strings.browserLaunchError, modifier = Modifier.padding(12.dp))
        if (appConfig.appEnvironment.isRelease()) {
            SettingsCheckbox(
                title = strings.settingsCrashReporting,
                checked = isCrashReportingEnabled,
                onCheckedChange = onCrashReportingToggled,
            )
        }
        DefaultButton(
            onClick = {
                val opened = openDesktopMailSafely(
                    userFeedbackReporter.getEmailUrl(
                        subject = strings.issueContentTitle,
                        content = strings.issueContentTemplate,
                    ),
                )
                if (!opened) showOpenError = true
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) { Text(strings.reportIssueButton) }
        if (FeatureFlags.ENABLE_FAQ) {
            Link(
                text = strings.aboutMenuFaq,
                onClick = {
                    val languageCode = java.util.Locale.getDefault().language
                    openExternalUrl("https://feedflow.dev/$languageCode/faq")
                },
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Text(strings.aboutTheApp, modifier = Modifier.padding(vertical = 12.dp))
        Link(strings.openWebsiteButton, onClick = {
            openExternalUrl(FEED_FLOW_WEBSITE)
        }, modifier = Modifier.padding(vertical = 8.dp))
        Link(
            strings.aboutMenuContributeTranslations,
            onClick = { openExternalUrl(TRANSLATION_WEBSITE) },
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Link(
            strings.openSourceLicenses,
            onClick = { showLicenses = true },
            modifier = Modifier.padding(vertical = 8.dp).focusRequester(licensesFocus),
        )
        appConfig.version?.let { version ->
            Text(strings.aboutAppVersion(version), modifier = Modifier.padding(vertical = 12.dp))
        }
        Link("${strings.authorLabel} Marco Gomiero", onClick = {
            openExternalUrl(MG_WEBSITE)
        }, modifier = Modifier.padding(vertical = 8.dp))
    }
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
