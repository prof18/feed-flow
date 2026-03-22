package com.prof18.feedflow.android.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun BackgroundSyncRestrictionsSection(
    syncOnlyOnWifi: Boolean,
    syncOnlyWhenCharging: Boolean,
    onSyncOnlyOnWifiChange: (Boolean) -> Unit,
    onSyncOnlyWhenChargingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val strings = LocalFeedFlowStrings.current

    Column(modifier = modifier) {
        if (showHeader) {
            Text(
                text = strings.settingsBackgroundSyncRestrictionsTitle,
                modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = strings.settingsBackgroundSyncRestrictionsDescription,
                modifier = Modifier.padding(horizontal = Spacing.regular),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingSwitchItem(
            title = strings.settingsBackgroundSyncWifiOnly,
            icon = Icons.Outlined.Wifi,
            isChecked = syncOnlyOnWifi,
            onCheckedChange = onSyncOnlyOnWifiChange,
        )

        SettingSwitchItem(
            title = strings.settingsBackgroundSyncChargingOnly,
            icon = Icons.Outlined.BatteryChargingFull,
            isChecked = syncOnlyWhenCharging,
            onCheckedChange = onSyncOnlyWhenChargingChange,
        )
    }
}
