package com.prof18.feedflow.android.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun SyncPeriodSelector(
    currentPeriod: SyncPeriod,
    onPeriodSelected: (SyncPeriod) -> Unit,
    modifier: Modifier = Modifier,
    showNeverSync: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Row(
        verticalAlignment = Alignment.Companion.CenterVertically,
        modifier = modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.small)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.Sync,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.Companion.weight(1f),
        ) {
            Text(
                text = strings.settingsSyncPeriod,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentPeriod) {
                    SyncPeriod.NEVER -> strings.settingsSyncPeriodNever
                    SyncPeriod.FIFTEEN_MINUTES -> strings.settingsSyncPeriodFifteenMinutes
                    SyncPeriod.THIRTY_MINUTES -> strings.settingsSyncPeriodThirtyMinutes
                    SyncPeriod.ONE_HOUR -> strings.settingsSyncPeriodOneHour
                    SyncPeriod.TWO_HOURS -> strings.settingsSyncPeriodTwoHours
                    SyncPeriod.SIX_HOURS -> strings.settingsSyncPeriodSixHours
                    SyncPeriod.TWELVE_HOURS -> strings.settingsSyncPeriodTwelveHours
                    SyncPeriod.ONE_DAY -> strings.settingsSyncPeriodOneDay
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        SyncPeriodDialog(
            currentPeriod = currentPeriod,
            onPeriodSelected = onPeriodSelected,
            showNeverSync = showNeverSync,
            dismissDialog = { showDialog = false },
        )
    }
}
