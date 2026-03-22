package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.bazqux.BazquxSyncScreen
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.accounts.feedbin.FeedbinSyncScreen
import com.prof18.feedflow.desktop.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveSyncScreen
import com.prof18.feedflow.desktop.accounts.icloud.ICloudSyncScreen
import com.prof18.feedflow.desktop.accounts.miniflux.MinifluxSyncScreen
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch

@Composable
internal fun AccountsWindow(
    onCloseRequest: () -> Unit,
) {
    DesktopDialogWindow(
        title = LocalFeedFlowStrings.current.settingsAccounts,
        size = DpSize(900.dp, 800.dp),
        onCloseRequest = onCloseRequest,
    ) { modifier ->
        val navigator = rememberListDetailPaneScaffoldNavigator<SyncAccounts>()
        val coroutineScope = rememberCoroutineScope()
        val showNavigateBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] != PaneAdaptedValue.Expanded

        LaunchedEffect(Unit) {
            while (navigator.canNavigateBack(BackNavigationBehavior.PopLatest)) {
                navigator.navigateBack(BackNavigationBehavior.PopLatest)
            }
        }

        val navigateToDetail: suspend (SyncAccounts) -> Unit = { account ->
            if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
                navigator.navigateBack(BackNavigationBehavior.PopLatest)
            }
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, account)
        }

        ListDetailPaneScaffold(
            modifier = modifier,
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                Box(modifier = Modifier.fillMaxSize()) {
                    AccountsScreen(
                        selectedAccount = navigator.currentDestination?.contentKey,
                        navigateToDropboxSync = {
                            coroutineScope.launch { navigateToDetail(SyncAccounts.DROPBOX) }
                        },
                        navigateToGoogleDriveSync = {
                            coroutineScope.launch { navigateToDetail(SyncAccounts.GOOGLE_DRIVE) }
                        },
                        navigateToICloudSync = {
                            coroutineScope.launch { navigateToDetail(SyncAccounts.ICLOUD) }
                        },
                        navigateToFreshRssSync = {
                            coroutineScope.launch { navigateToDetail(SyncAccounts.FRESH_RSS) }
                        },
                        navigateToMinifluxSync = {
                            coroutineScope.launch { navigateToDetail(SyncAccounts.MINIFLUX) }
                        },
                        navigateToBazquxSync = {
                            coroutineScope.launch { navigateToDetail(SyncAccounts.BAZQUX) }
                        },
                        navigateToFeedbinSync = {
                            coroutineScope.launch { navigateToDetail(SyncAccounts.FEEDBIN) }
                        },
                    )
                }
            },
            detailPane = {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navigateBack = {
                        coroutineScope.launch {
                            navigator.navigateBack(BackNavigationBehavior.PopLatest)
                        }
                        Unit
                    }
                    when (navigator.currentDestination?.contentKey) {
                        SyncAccounts.DROPBOX -> DropboxSyncScreen(
                            navigateBack = navigateBack,
                            showNavigateBack = showNavigateBack,
                        )
                        SyncAccounts.GOOGLE_DRIVE -> GoogleDriveSyncScreen(
                            navigateBack = navigateBack,
                            showNavigateBack = showNavigateBack,
                        )
                        SyncAccounts.ICLOUD -> ICloudSyncScreen(
                            navigateBack = navigateBack,
                            showNavigateBack = showNavigateBack,
                        )
                        SyncAccounts.FRESH_RSS -> FreshRssSyncScreen(
                            navigateBack = navigateBack,
                            showNavigateBack = showNavigateBack,
                        )
                        SyncAccounts.MINIFLUX -> MinifluxSyncScreen(
                            navigateBack = navigateBack,
                            showNavigateBack = showNavigateBack,
                        )
                        SyncAccounts.BAZQUX -> BazquxSyncScreen(
                            navigateBack = navigateBack,
                            showNavigateBack = showNavigateBack,
                        )
                        SyncAccounts.FEEDBIN -> FeedbinSyncScreen(
                            navigateBack = navigateBack,
                            showNavigateBack = showNavigateBack,
                        )
                        else -> {}
                    }
                }
            },
        )
    }
}
