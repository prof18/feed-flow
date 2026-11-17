package com.prof18.feedflow.android.settings.notifications

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.android.settings.sync.label
import com.prof18.feedflow.android.ui.preview.PreviewPhone
import com.prof18.feedflow.android.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.android.ui.settings.SettingDropdownOption
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.collections.immutable.toPersistentList

/**
 * Background refresh period, editable in place. At Never it explains why notifications can't arrive.
 * Wi-Fi and charging conditions stay in Sync & refresh.
 */
@Composable
internal fun BackgroundRefreshRow(
    syncPeriod: SyncPeriod,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current

    CompactSettingDropdownRow(
        modifier = modifier,
        title = strings.settingsSyncPeriod,
        currentValue = syncPeriod,
        options = SyncPeriod.entries.map { SettingDropdownOption(it, it.label(strings)) }.toPersistentList(),
        onOptionSelected = onSyncPeriodSelected,
        supportingText = if (syncPeriod == SyncPeriod.NEVER) {
            {
                Text(
                    text = strings.settingsNotificationsRefreshOff,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            null
        },
    )
}

@PreviewPhone
@Composable
private fun BackgroundRefreshRowOffPreview() {
    FeedFlowTheme {
        BackgroundRefreshRow(
            syncPeriod = SyncPeriod.NEVER,
            onSyncPeriodSelected = {},
        )
    }
}

@PreviewPhone
@Composable
private fun BackgroundRefreshRowOnPreview() {
    FeedFlowTheme {
        BackgroundRefreshRow(
            syncPeriod = SyncPeriod.ONE_HOUR,
            onSyncPeriodSelected = {},
        )
    }
}
