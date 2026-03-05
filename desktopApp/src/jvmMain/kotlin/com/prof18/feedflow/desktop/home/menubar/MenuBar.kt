package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.presentation.MenuBarViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    state: MenuBarState,
    actions: MenuBarActions,
    onNavigateToBlockedWords: () -> Unit,
) {
    val isMacOS = getDesktopOS().isMacOs()
    val userFeedbackReporter = DI.koin.get<UserFeedbackReporter>()
    val menuBarViewModel = koinViewModel<MenuBarViewModel>()
    val settingsState by menuBarViewModel.state.collectAsState()
    val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
    val emailContent = LocalFeedFlowStrings.current.issueContentTemplate

    val feedMenuCallbacks = FeedMenuCallbacks(
        onAddFeed = { actions.onAddFeedClick() },
        onEditFeed = { _ -> },
        onBlockedWordsClick = onNavigateToBlockedWords,
    )
    val viewMenuCallbacks = ViewMenuCallbacks(
        onThemeModeSelected = menuBarViewModel::updateThemeMode,
        onShowReadItemsToggled = menuBarViewModel::updateShowReadItemsOnTimeline,
        onFeedOrderSelected = menuBarViewModel::updateFeedOrder,
    )
    val helpMenuCallbacks = HelpMenuCallbacks(
        onBugReportClick = {
            val desktop = java.awt.Desktop.getDesktop()
            val uri = java.net.URI.create(
                userFeedbackReporter.getEmailUrl(
                    subject = emailSubject,
                    content = emailContent,
                ),
            )
            desktop.mail(uri)
        },
    )

    MenuBar {
        FileMenu(
            isMacOS = isMacOS,
            state = state,
            actions = actions,
        )

        FeedMenu(
            isMacOS = isMacOS,
            state = state,
            callbacks = feedMenuCallbacks,
        )

        ViewMenu(
            settingsState = settingsState,
            callbacks = viewMenuCallbacks,
        )

        HelpMenu(
            callbacks = helpMenuCallbacks,
        )
    }
}

data class MenuBarActions(
    val onRefreshClick: () -> Unit,
    val onAddFeedClick: () -> Unit,
    val onMarkAllReadClick: () -> Unit,
    val onImportExportClick: () -> Unit,
    val onClearOldFeedClick: () -> Unit,
    val onForceRefreshClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val deleteFeeds: () -> Unit,
    val onBackupClick: () -> Unit,
    val onExitClick: () -> Unit,
)

data class MenuBarState(
    val showDebugMenu: Boolean,
    val feedFilter: FeedFilter,
    val isSyncUploadRequired: Boolean,
)
