package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.rememberScrollbarState
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.settings.SettingsGroup
import com.prof18.feedflow.desktop.testing.AccountE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.desktop.ui.components.rememberDesktopSidebarItemStyle
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.foundation.lazy.SelectableColumnKeybindings
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListKey
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.lazy.SingleSelectionLazyColumn
import org.jetbrains.jewel.foundation.lazy.SingleSelectionLazyListState
import org.jetbrains.jewel.foundation.lazy.tree.DefaultSelectableLazyColumnEventAction
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.SimpleListItem
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.SimpleListItemStyle

/** Shared accounts overview and navigation. Selecting a provider only opens its detail slot. */
@Composable
internal fun DesktopAccountsContent(
    currentAccount: SyncAccounts,
    supportedAccounts: ImmutableList<SyncAccounts>,
    modifier: Modifier = Modifier,
    detailContent: @Composable (SyncAccounts, () -> Unit, Boolean, CredentialsState) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val providers = desktopAccountPresentations(currentAccount, supportedAccounts, strings)
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var showServices by rememberSaveable { mutableStateOf(true) }
    val selected = providers.firstOrNull { it.account.name == selectedName && it.isEnabled }
    val accountScope = rememberCoroutineScope()
    val credentialDrafts = remember { mutableMapOf<SyncAccounts, CredentialsState>() }
    val providerScrollState = rememberLazyListState()
    val providerListState = remember(providerScrollState) {
        SingleSelectionLazyListState(
            providerScrollState,
            selected?.account ?: providers.firstOrNull { it.isEnabled }?.account,
        )
    }
    val select: (SyncAccounts) -> Unit = { account ->
        if (providers.any { it.account == account && it.isEnabled }) {
            selectedName = account.name
            showServices = false
        }
    }
    LaunchedEffect(selectedName, currentAccount, supportedAccounts) {
        if (selectedName != null && selected == null) {
            selectedName = null
            showServices = true
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= ACCOUNTS_WIDE_BREAKPOINT
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (wide || showServices || selected == null) {
                AccountsOverview(
                    currentAccount = currentAccount,
                    providers = providers,
                    selectedAccount = selected?.account,
                    listState = providerListState,
                    onSelect = select,
                    modifier = if (wide) Modifier.width(240.dp).fillMaxHeight() else Modifier.fillMaxSize(),
                )
            }
            if (wide || (!showServices && selected != null)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selected == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(strings.desktopAccountsChooseService, Modifier.padding(24.dp))
                        }
                    } else {
                        if (!wide) {
                            OutlinedButton(onClick = { showServices = true }) {
                                Text(strings.desktopAccountsBackToServices)
                            }
                        }
                        Text(selected.description, Modifier.padding(horizontal = 16.dp))
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            key(selected.account) {
                                val credentials = credentialDrafts.getOrPut(selected.account) {
                                    CredentialsState(accountScope)
                                }
                                detailContent(selected.account, { showServices = true }, false, credentials)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsOverview(
    currentAccount: SyncAccounts,
    providers: ImmutableList<DesktopAccountPresentation>,
    selectedAccount: SyncAccounts?,
    listState: SingleSelectionLazyListState,
    onSelect: (SyncAccounts) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsGroup(strings.settingsAccounts)
        Text(currentAccount.desktopTitle(strings), fontWeight = FontWeight.SemiBold)
        Text(
            if (currentAccount == SyncAccounts.LOCAL) {
                strings.desktopAccountsLocalStatus
            } else {
                strings.desktopAccountsConnectedStatus
            },
            modifier = Modifier.testTag(ACCOUNT_CURRENT_STATUS_TAG),
        )
        if (providers.any { it.account == currentAccount }) {
            OutlinedButton(onClick = { onSelect(currentAccount) }, modifier = Modifier.testTag(ACCOUNT_MANAGE_TAG)) {
                Text(strings.desktopAccountsManageButton)
            }
        }
        Text(
            if (currentAccount == SyncAccounts.LOCAL) {
                strings.desktopAccountsOverviewDescription
            } else {
                strings.desktopAccountsSingleProviderExplanation
            },
        )
        SettingsGroup(strings.desktopAccountsServicesTitle)
        AccountsProviderList(
            providers = providers,
            selectedAccount = selectedAccount,
            listState = listState,
            onSelect = onSelect,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun AccountsProviderList(
    providers: ImmutableList<DesktopAccountPresentation>,
    selectedAccount: SyncAccounts?,
    listState: SingleSelectionLazyListState,
    onSelect: (SyncAccounts) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestProviders by rememberUpdatedState(providers)
    val latestSelect by rememberUpdatedState(onSelect)
    val style = rememberDesktopSidebarItemStyle()
    fun select(key: Any?) {
        latestProviders.firstOrNull { it.account == key && it.isEnabled }?.let { latestSelect(it.account) }
    }
    LaunchedEffect(selectedAccount, providers.map { it.account to it.isEnabled }) {
        val key = selectedAccount ?: (listState.selectedKeys.firstOrNull() as? SyncAccounts)
            ?.takeIf { account -> providers.any { it.account == account && it.isEnabled } }
            ?: providers.firstOrNull { it.isEnabled }?.account
        listState.selectedKeys = setOfNotNull(key)
    }
    val pointerActions = remember {
        object : DefaultSelectableLazyColumnEventAction() {
            override fun handlePointerEventPress(
                pointerEvent: PointerEvent,
                keybindings: SelectableColumnKeybindings,
                selectableLazyListState: SelectableLazyListState,
                selectionMode: SelectionMode,
                allKeys: List<SelectableLazyListKey>,
                key: Any,
            ) {
                if (!pointerEvent.buttons.isPrimaryPressed || pointerEvent.changes.any { it.isConsumed }) return
                super.handlePointerEventPress(
                    pointerEvent,
                    keybindings,
                    selectableLazyListState,
                    selectionMode,
                    allKeys,
                    key,
                )
                select(key)
            }
        }
    }
    Box(modifier) {
        SingleSelectionLazyColumn(
            state = listState,
            pointerEventActions = pointerActions,
            modifier = Modifier.fillMaxSize().padding(end = 12.dp)
                .testTag(ACCOUNT_PROVIDERS_TAG)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.Spacebar)) {
                        select(listState.selectedKeys.firstOrNull())
                        true
                    } else {
                        false
                    }
                },
        ) {
            providers.forEach { provider ->
                item(key = provider.account, selectable = provider.isEnabled) {
                    AccountProviderRow(
                        provider = provider,
                        state = ListItemState(isSelected = isSelected, isActive = isActive && provider.isEnabled),
                        style = style,
                    )
                }
            }
        }
        if (listState.canScrollBackward || listState.canScrollForward) {
            FeedFlowVerticalScrollbar(rememberScrollbarState(listState.lazyListState))
        }
    }
}

@Composable
private fun AccountProviderRow(
    provider: DesktopAccountPresentation,
    state: ListItemState,
    style: SimpleListItemStyle,
) {
    SimpleListItem(
        state = state,
        style = style,
        height = 64.dp,
        modifier = Modifier.fillMaxWidth()
            .testTag(AccountE2eIds.providerRow(provider.account))
            .alpha(if (provider.isEnabled) 1f else DISABLED_PROVIDER_ALPHA)
            .semantics { if (!provider.isEnabled) disabled() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            provider.account.desktopIcon()?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                        .testTag("${AccountE2eIds.providerRow(provider.account)}-icon"),
                    tint = if (provider.account.hasMonochromeIcon()) JewelTheme.contentColor else Color.Unspecified,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(provider.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(provider.status, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal const val ACCOUNT_PROVIDERS_TAG = "account-providers"
internal const val ACCOUNT_CURRENT_STATUS_TAG = "account-current-status"
internal const val ACCOUNT_MANAGE_TAG = "account-manage-current"
private val ACCOUNTS_WIDE_BREAKPOINT = 680.dp
private const val DISABLED_PROVIDER_ALPHA = 0.6f
