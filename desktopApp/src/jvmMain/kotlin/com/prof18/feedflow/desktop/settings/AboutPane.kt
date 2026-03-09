package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.prof18.feedflow.shared.ui.about.AboutButtonItem
import com.prof18.feedflow.shared.ui.about.AboutTextItem
import com.prof18.feedflow.shared.ui.about.AuthorText
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AboutPane(
    isCrashReportingEnabled: Boolean,
    onCrashReportingToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val appConfig = remember { DI.koin.get<DesktopConfig>() }
    val uriHandler = LocalUriHandler.current
    var showLicenses by remember { mutableStateOf(false) }

    if (showLicenses) {
        LicensesScreen(
            onBackClick = { showLicenses = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (appConfig.appEnvironment.isRelease()) {
            SettingSwitchItem(
                title = strings.settingsCrashReporting,
                icon = Icons.Outlined.Report,
                isChecked = isCrashReportingEnabled,
                onCheckedChange = onCrashReportingToggled,
            )
        }

        if (FeatureFlags.ENABLE_FAQ) {
            SettingItem(
                title = strings.aboutMenuFaq,
                icon = Icons.Outlined.QuestionMark,
                onClick = {
                    val languageCode = java.util.Locale.getDefault().language
                    runCatching {
                        uriHandler.openUri("https://feedflow.dev/$languageCode/faq")
                    }
                },
            )
        }

        AboutTextItem(modifier = Modifier.padding(Spacing.regular))

        AboutButtonItem(
            onClick = { uriHandler.openUri(FEED_FLOW_WEBSITE) },
            buttonText = strings.openWebsiteButton,
        )

        AboutButtonItem(
            onClick = { uriHandler.openUri(TRANSLATION_WEBSITE) },
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
            nameClicked = { uriHandler.openUri(MG_WEBSITE) },
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
        )
    }
}
