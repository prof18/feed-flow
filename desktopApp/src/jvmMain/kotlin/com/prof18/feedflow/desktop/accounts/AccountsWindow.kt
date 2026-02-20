package com.prof18.feedflow.desktop.accounts

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.prof18.feedflow.desktop.accounts.bazqux.BazquxSyncScreen
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.accounts.feedbin.FeedbinSyncScreen
import com.prof18.feedflow.desktop.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveSyncScreen
import com.prof18.feedflow.desktop.accounts.icloud.ICloudSyncScreen
import com.prof18.feedflow.desktop.accounts.miniflux.MinifluxSyncScreen
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import kotlinx.serialization.Serializable

@Composable
internal fun AccountsWindow(
    visible: Boolean,
    onCloseRequest: () -> Unit,
) {
    DesktopDialogWindow(
        title = LocalFeedFlowStrings.current.settingsAccounts,
        size = DpSize(600.dp, 700.dp),
        visible = visible,
        onCloseRequest = onCloseRequest,
    ) { modifier ->
        val backStack = remember { mutableStateListOf<NavKey>(AccountsHome) }
        val reduceMotionEnabled = LocalReduceMotion.current

        LaunchedEffect(visible) {
            if (visible) {
                backStack.clear()
                backStack.add(AccountsHome)
            }
        }

        val navigateBack: () -> Unit = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else {
                onCloseRequest()
            }
        }

        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            onBack = navigateBack,
            transitionSpec = {
                if (reduceMotionEnabled) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 150))
                }
            },
            popTransitionSpec = {
                if (reduceMotionEnabled) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 150))
                }
            },
            entryProvider = entryProvider {
                entry<AccountsHome> {
                    AccountsScreen(
                        navigateToDropboxSync = { backStack.add(AccountsDropboxSync) },
                        navigateToGoogleDriveSync = { backStack.add(AccountsGoogleDriveSync) },
                        navigateToICloudSync = { backStack.add(AccountsICloudSync) },
                        navigateToFreshRssSync = { backStack.add(AccountsFreshRssSync) },
                        navigateToMinifluxSync = { backStack.add(AccountsMinifluxSync) },
                        navigateToBazquxSync = { backStack.add(AccountsBazquxSync) },
                        navigateToFeedbinSync = { backStack.add(AccountsFeedbinSync) },
                    )
                }
                entry<AccountsDropboxSync> {
                    DropboxSyncScreen(navigateBack = navigateBack)
                }
                entry<AccountsGoogleDriveSync> {
                    GoogleDriveSyncScreen(navigateBack = navigateBack)
                }
                entry<AccountsICloudSync> {
                    ICloudSyncScreen(navigateBack = navigateBack)
                }
                entry<AccountsFreshRssSync> {
                    FreshRssSyncScreen(navigateBack = navigateBack)
                }
                entry<AccountsMinifluxSync> {
                    MinifluxSyncScreen(navigateBack = navigateBack)
                }
                entry<AccountsBazquxSync> {
                    BazquxSyncScreen(navigateBack = navigateBack)
                }
                entry<AccountsFeedbinSync> {
                    FeedbinSyncScreen(navigateBack = navigateBack)
                }
            },
        )
    }
}

@Serializable private data object AccountsHome : NavKey

@Serializable private data object AccountsDropboxSync : NavKey

@Serializable private data object AccountsGoogleDriveSync : NavKey

@Serializable private data object AccountsICloudSync : NavKey

@Serializable private data object AccountsFreshRssSync : NavKey

@Serializable private data object AccountsMinifluxSync : NavKey

@Serializable private data object AccountsBazquxSync : NavKey

@Serializable private data object AccountsFeedbinSync : NavKey
