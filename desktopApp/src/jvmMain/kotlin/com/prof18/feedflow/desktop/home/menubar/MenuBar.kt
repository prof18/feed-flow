package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.DesktopWorkspaceCommandAvailability
import com.prof18.feedflow.desktop.main.LocalDesktopMenuCommands
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.utils.openDesktopMailSafely
import com.prof18.feedflow.shared.presentation.MenuBarViewModel
import com.prof18.feedflow.shared.utils.UserFeedbackReporter
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    state: MenuBarState,
    actions: MenuBarActions,
) {
    val isMacOS = getDesktopOS().isMacOs()
    val menuCommands = LocalDesktopMenuCommands.current
    val userFeedbackReporter = DI.koin.get<UserFeedbackReporter>()
    val menuBarViewModel = koinViewModel<MenuBarViewModel>()
    val settingsState by menuBarViewModel.state.collectAsState()
    val emailSubject = LocalFeedFlowStrings.current.issueContentTitle
    val emailContent = LocalFeedFlowStrings.current.issueContentTemplate

    val feedMenuCallbacks = FeedMenuCallbacks(
        onAddFeed = { actions.onAddFeedClick() },
        onEditFeed = actions.onEditFeedClick,
        onFeedSuggestionsClick = actions.onFeedSuggestionsClick,
    )
    val viewMenuCallbacks = ViewMenuCallbacks(
        onShowReadItemsToggled = menuBarViewModel::updateShowReadItemsOnTimeline,
        onFeedOrderSelected = menuBarViewModel::updateFeedOrder,
    )
    val helpMenuCallbacks = HelpMenuCallbacks(
        onBugReportClick = {
            openDesktopMailSafely(
                userFeedbackReporter.getEmailUrl(
                    subject = emailSubject,
                    content = emailContent,
                ),
            )
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
            isMacOS = isMacOS,
            settingsState = settingsState,
            callbacks = viewMenuCallbacks,
            commands = menuCommands,
        )

        ArticleMenu(isMacOS = isMacOS, article = menuCommands.article)

        if (isMacOS) {
            WindowMenu(window)
        }

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
    val onSettingsClick: () -> Unit,
    val deleteFeeds: () -> Unit,
    val onBackupClick: () -> Unit,
    val onExitClick: () -> Unit,
    val onEditFeedClick: (FeedSource) -> Unit = {},
    val onFeedSuggestionsClick: () -> Unit = {},
)

data class MenuBarState(
    val showDebugMenu: Boolean,
    val feedFilter: FeedFilter,
    val isSyncUploadRequired: Boolean,
    val commands: DesktopWorkspaceCommandAvailability = DesktopWorkspaceCommandAvailability(),
)
