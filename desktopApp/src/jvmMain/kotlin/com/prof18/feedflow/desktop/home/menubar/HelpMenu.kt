package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuBarScope
import com.prof18.feedflow.core.utils.FeatureFlags
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isLinux
import com.prof18.feedflow.desktop.utils.openDesktopUriSafely
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun MenuBarScope.HelpMenu(
    callbacks: HelpMenuCallbacks,
) {
    Menu(LocalFeedFlowStrings.current.settingsHelpTitle, mnemonic = 'H') {
        if (FeatureFlags.ENABLE_FAQ) {
            Item(
                text = LocalFeedFlowStrings.current.aboutMenuFaq,
                onClick = {
                    val languageCode = java.util.Locale.getDefault().language
                    openDesktopUriSafely("https://feedflow.dev/$languageCode/faq")
                },
            )
        }

        Item(
            text = LocalFeedFlowStrings.current.reportIssueButton,
            onClick = callbacks.onBugReportClick,
        )

        if (getDesktopOS().isLinux()) {
            Separator()

            Item(
                text = LocalFeedFlowStrings.current.supportTheProject,
                onClick = {
                    openDesktopUriSafely("https://www.paypal.me/MarcoGomiero")
                },
            )
        }
    }
}

internal data class HelpMenuCallbacks(
    val onBugReportClick: () -> Unit,
)
