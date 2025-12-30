package com.prof18.feedflow.android.settings.about

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.CrashlyticsHelper
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.shared.presentation.AboutAndSupportSettingsViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutAndSupportScreen(
    navigateBack: () -> Unit,
    onAboutClick: () -> Unit,
) {
    val viewModel = koinViewModel<AboutAndSupportSettingsViewModel>()
    val appConfig = koinInject<AppConfig>()
    val userFeedbackReporter = koinInject<UserFeedbackReporter>()
    val browserManager = koinInject<BrowserManager>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val strings = LocalFeedFlowStrings.current

    AboutAndSupportScreenContent(
        navigateBack = navigateBack,
        onAboutClick = onAboutClick,
        showCrashReporting = appConfig.isLoggingEnabled,
        isCrashReportingEnabled = state.isCrashReportingEnabled,
        onCrashReportingEnabled = { enabled ->
            viewModel.updateCrashReporting(enabled)
            if (appConfig.isLoggingEnabled) {
                CrashlyticsHelper.setCollectionEnabled(enabled)
            }
        },
        onReportIssueClick = {
            val uri = userFeedbackReporter.getEmailUrl(
                subject = strings.issueContentTitle,
                content = strings.issueContentTemplate,
            ).toUri()
            val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
            context.startActivity(Intent.createChooser(emailIntent, strings.issueReportTitle))
        },
        onFaqClick = {
            val languageCode = java.util.Locale.getDefault().language
            val faqUrl = "https://feedflow.dev/$languageCode/faq"
            browserManager.openWithInAppBrowser(faqUrl, context)
        },
    )
}
