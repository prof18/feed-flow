package com.prof18.feedflow.android.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun WidgetSyncStatus(
    syncPeriod: SyncPeriod,
    onManageSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val isDisabled = syncPeriod == SyncPeriod.NEVER
    Surface(
        modifier = modifier.testTag("widget_sync_status"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.xsmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Icon(
                imageVector = if (isDisabled) Icons.Outlined.SyncDisabled else Icons.Outlined.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDisabled) strings.widgetSyncDisabled else strings.settingsSyncPeriod,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!isDisabled) {
                    Text(
                        text = when (syncPeriod) {
                            SyncPeriod.NEVER -> strings.settingsSyncPeriodNever
                            SyncPeriod.FIFTEEN_MINUTES -> strings.settingsSyncPeriodFifteenMinutes
                            SyncPeriod.THIRTY_MINUTES -> strings.settingsSyncPeriodThirtyMinutes
                            SyncPeriod.ONE_HOUR -> strings.settingsSyncPeriodOneHour
                            SyncPeriod.TWO_HOURS -> strings.settingsSyncPeriodTwoHours
                            SyncPeriod.SIX_HOURS -> strings.settingsSyncPeriodSixHours
                            SyncPeriod.TWELVE_HOURS -> strings.settingsSyncPeriodTwelveHours
                            SyncPeriod.ONE_DAY -> strings.settingsSyncPeriodOneDay
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(
                onClick = onManageSync,
                modifier = Modifier.testTag("widget_sync_action"),
            ) {
                Text(if (isDisabled) strings.widgetEnableSync else strings.widgetSyncSettingsAction)
            }
        }
    }
}
