package com.prof18.feedflow.desktop.accounts

import androidx.compose.ui.graphics.vector.ImageVector
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.icons.Bazqux
import com.prof18.feedflow.desktop.accounts.icons.Cloud
import com.prof18.feedflow.desktop.accounts.icons.Dropbox
import com.prof18.feedflow.desktop.accounts.icons.Feedbin
import com.prof18.feedflow.desktop.accounts.icons.FreshRSS
import com.prof18.feedflow.desktop.accounts.icons.GoogleDriveLogo
import com.prof18.feedflow.desktop.accounts.icons.Miniflux
import com.prof18.feedflow.i18n.FeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun desktopAccountPresentations(
    currentAccount: SyncAccounts,
    supportedAccounts: List<SyncAccounts>,
    strings: FeedFlowStrings,
): ImmutableList<DesktopAccountPresentation> = supportedAccounts
    .filter { it != SyncAccounts.LOCAL }
    .distinct()
    .map { account ->
        val isCurrent = account == currentAccount
        val enabled = currentAccount == SyncAccounts.LOCAL || isCurrent
        DesktopAccountPresentation(
            account = account,
            title = account.desktopTitle(strings),
            description = if (account.isBackupProvider()) {
                strings.desktopAccountsBackupDescription
            } else {
                strings.desktopAccountsReaderDescription
            },
            status = when {
                isCurrent -> strings.desktopAccountsConnectedStatus
                enabled -> strings.desktopAccountsAvailableStatus
                else -> strings.desktopAccountsUnavailableStatus
            },
            isEnabled = enabled,
        )
    }
    .toImmutableList()

internal fun SyncAccounts.desktopTitle(strings: FeedFlowStrings): String = when (this) {
    SyncAccounts.DROPBOX -> strings.desktopAccountsDropboxTitle
    SyncAccounts.GOOGLE_DRIVE -> strings.desktopAccountsGoogleDriveTitle
    SyncAccounts.ICLOUD -> strings.desktopAccountsIcloudTitle
    SyncAccounts.FRESH_RSS -> strings.desktopAccountsFreshRssTitle
    SyncAccounts.MINIFLUX -> strings.desktopAccountsMinifluxTitle
    SyncAccounts.BAZQUX -> strings.desktopAccountsBazquxTitle
    SyncAccounts.FEEDBIN -> strings.desktopAccountsFeedbinTitle
    SyncAccounts.LOCAL -> strings.desktopAccountsLocalTitle
}

internal fun SyncAccounts.desktopIcon(): ImageVector? = when (this) {
    SyncAccounts.DROPBOX -> Dropbox
    SyncAccounts.GOOGLE_DRIVE -> GoogleDriveLogo
    SyncAccounts.ICLOUD -> Cloud
    SyncAccounts.FRESH_RSS -> FreshRSS
    SyncAccounts.MINIFLUX -> Miniflux
    SyncAccounts.BAZQUX -> Bazqux
    SyncAccounts.FEEDBIN -> Feedbin
    SyncAccounts.LOCAL -> null
}

internal fun SyncAccounts.isBackupProvider(): Boolean =
    this == SyncAccounts.DROPBOX || this == SyncAccounts.GOOGLE_DRIVE || this == SyncAccounts.ICLOUD

internal fun SyncAccounts.hasMonochromeIcon(): Boolean =
    this == SyncAccounts.DROPBOX || this == SyncAccounts.ICLOUD ||
        this == SyncAccounts.MINIFLUX || this == SyncAccounts.FEEDBIN

internal data class DesktopAccountPresentation(
    val account: SyncAccounts,
    val title: String,
    val description: String,
    val status: String,
    val isEnabled: Boolean,
)
