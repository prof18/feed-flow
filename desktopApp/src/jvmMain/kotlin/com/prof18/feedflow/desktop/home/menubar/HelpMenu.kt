package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuBarScope
import com.prof18.feedflow.core.utils.FeatureFlags
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isLinux
import com.prof18.feedflow.shared.presentation.model.MenuBarSettingsState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import java.awt.Desktop
import java.net.URI

@Composable
internal fun MenuBarScope.HelpMenu(
    settingsState: MenuBarSettingsState,
    callbacks: HelpMenuCallbacks,
) {
    Menu(LocalFeedFlowStrings.current.settingsHelpTitle, mnemonic = 'H') {
        if (FeatureFlags.ENABLE_FAQ) {
            Item(
                text = LocalFeedFlowStrings.current.aboutMenuFaq,
                onClick = {
                    runCatching {
                        val languageCode = java.util.Locale.getDefault().language
                        val faqUrl = "https://feedflow.dev/$languageCode/faq"
                        Desktop.getDesktop().browse(URI(faqUrl))
                    }
                },
            )
        }

        Item(
            text = LocalFeedFlowStrings.current.reportIssueButton,
            onClick = callbacks.onBugReportClick,
        )

        CheckboxItem(
            text = LocalFeedFlowStrings.current.settingsCrashReporting,
            checked = settingsState.isCrashReportingEnabled,
            onCheckedChange = callbacks.onCrashReportingToggled,
        )

        Separator()

        if (getDesktopOS().isLinux()) {
            Item(
                text = LocalFeedFlowStrings.current.supportTheProject,
                onClick = {
                    runCatching {
                        Desktop.getDesktop().browse(URI("https://www.paypal.me/MarcoGomiero"))
                    }
                },
            )
        }

        Item(
            text = LocalFeedFlowStrings.current.aboutButton,
            onClick = callbacks.onAboutClick,
        )
    }
}

internal data class HelpMenuCallbacks(
    val onBugReportClick: () -> Unit,
    val onCrashReportingToggled: (Boolean) -> Unit,
    val onAboutClick: () -> Unit,
)
