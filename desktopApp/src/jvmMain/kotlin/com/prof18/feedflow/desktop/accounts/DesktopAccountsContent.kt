package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.settings.SettingsActionRow
import com.prof18.feedflow.desktop.settings.SettingsPage
import com.prof18.feedflow.desktop.settings.SettingsRow
import com.prof18.feedflow.desktop.settings.SettingsSection
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

/**
 * Accounts settings page. The overview lists the current account and the available services; opening a
 * service shows its connection details as a subpage. Opening a service never connects it.
 */
@Composable
internal fun DesktopAccountsContent(
    currentAccount: SyncAccounts,
    supportedAccounts: ImmutableList<SyncAccounts>,
    modifier: Modifier = Modifier,
    drafts: AccountDrafts = rememberAccountDrafts(),
    detailContent: @Composable (SyncAccounts, () -> Unit, Boolean, CredentialsState) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val providers = desktopAccountPresentations(currentAccount, supportedAccounts, strings)
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = providers.firstOrNull { it.account.name == selectedName && it.isEnabled }
    val overviewScrollState = rememberScrollState()
    val open: (SyncAccounts) -> Unit = { account ->
        if (providers.any { it.account == account && it.isEnabled }) selectedName = account.name
    }
    val back = { selectedName = null }
    LaunchedEffect(selectedName, currentAccount, supportedAccounts) {
        if (selectedName != null && selected == null) selectedName = null
    }

    Box(modifier.fillMaxSize()) {
        if (selected == null) {
            AccountsOverview(
                currentAccount = currentAccount,
                providers = providers,
                onOpen = open,
                scrollState = overviewScrollState,
            )
        } else {
            AccountDetailPage(provider = selected, onBack = back) {
                key(selected.account) {
                    detailContent(selected.account, back, false, drafts.forAccount(selected.account))
                }
            }
        }
    }
}

@Composable
private fun AccountsOverview(
    currentAccount: SyncAccounts,
    providers: ImmutableList<DesktopAccountPresentation>,
    onOpen: (SyncAccounts) -> Unit,
    scrollState: ScrollState,
) {
    val strings = LocalFeedFlowStrings.current
    val isLocal = currentAccount == SyncAccounts.LOCAL
    SettingsPage(title = strings.settingsAccounts, scrollState = scrollState) {
        SettingsSection(
            footer = if (isLocal) null else strings.desktopAccountsSingleProviderExplanation,
        ) {
            SettingsRow(
                title = currentAccount.desktopTitle(strings),
                leading = { AccountIcon(currentAccount, size = CURRENT_ICON_SIZE) },
                trailing = {
                    Text(
                        if (isLocal) strings.desktopAccountsLocalStatus else strings.desktopAccountsConnectedStatus,
                        modifier = Modifier.testTag(ACCOUNT_CURRENT_STATUS_TAG),
                        color = JewelTheme.globalColors.text.info,
                    )
                },
            )
            if (providers.any { it.account == currentAccount }) {
                SettingsActionRow(
                    title = strings.desktopAccountsManageButton,
                    onClick = { onOpen(currentAccount) },
                    showChevron = true,
                    modifier = Modifier.testTag(ACCOUNT_MANAGE_TAG),
                )
            }
        }

        Column(Modifier.testTag(ACCOUNT_PROVIDERS_TAG), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            val (backup, reader) = providers.partition { it.account.isBackupProvider() }
            ProviderSection(strings.desktopAccountsReaderServicesTitle, reader.toImmutableList(), onOpen)
            ProviderSection(strings.desktopAccountsBackupServicesTitle, backup.toImmutableList(), onOpen)
        }
    }
}

@Composable
private fun ProviderSection(
    title: String,
    providers: ImmutableList<DesktopAccountPresentation>,
    onOpen: (SyncAccounts) -> Unit,
) {
    if (providers.isEmpty()) return
    SettingsSection(title = title) {
        providers.forEach { provider ->
            SettingsActionRow(
                title = provider.title,
                supportingText = if (provider.isEnabled) provider.description else provider.status,
                onClick = { onOpen(provider.account) },
                enabled = provider.isEnabled,
                showChevron = provider.isEnabled,
                leading = { AccountIcon(provider.account, size = PROVIDER_ICON_SIZE) },
                modifier = Modifier
                    .testTag(AccountE2eIds.providerRow(provider.account))
                    .semantics { if (!provider.isEnabled) disabled() },
            )
        }
    }
}

@Composable
private fun AccountDetailPage(
    provider: DesktopAccountPresentation,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = JewelTheme.globalColors.outlines.focused,
                modifier = Modifier.size(18.dp),
            )
            Text(strings.desktopAccountsBackToServices, color = JewelTheme.globalColors.outlines.focused)
        }
        Row(
            modifier = Modifier.widthIn(max = DETAIL_MAX_WIDTH).fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AccountIcon(provider.account, size = DETAIL_ICON_SIZE)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    provider.title,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    ),
                )
                Text(provider.description, color = JewelTheme.globalColors.text.info)
            }
        }
        Box(Modifier.weight(1f).widthIn(max = DETAIL_MAX_WIDTH).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun AccountIcon(account: SyncAccounts, size: Dp) {
    val icon: ImageVector = account.desktopIcon() ?: Icons.Outlined.Computer
    val monochrome = account == SyncAccounts.LOCAL || account.hasMonochromeIcon(JewelTheme.isDark)
    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (account == SyncAccounts.LOCAL) {
                    Modifier
                } else {
                    Modifier.testTag("${AccountE2eIds.providerRow(account)}-icon")
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * ICON_GLYPH_RATIO),
            tint = if (monochrome) JewelTheme.globalColors.text.normal else Color.Unspecified,
        )
    }
}

internal const val ACCOUNT_PROVIDERS_TAG = "account-providers"
internal const val ACCOUNT_CURRENT_STATUS_TAG = "account-current-status"
internal const val ACCOUNT_MANAGE_TAG = "account-manage-current"
private val PROVIDER_ICON_SIZE = 28.dp
private val CURRENT_ICON_SIZE = 32.dp
private val DETAIL_ICON_SIZE = 44.dp
private val DETAIL_MAX_WIDTH = 648.dp
private const val ICON_GLYPH_RATIO = 0.8f
