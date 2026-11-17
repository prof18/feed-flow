package com.prof18.feedflow.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.ui.style.Spacing

@Composable
fun NotificationToggleRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.regular, vertical = SettingRowDefaults.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = title,
                    style = SettingRowDefaults.titleStyle,
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = {
                onCheckedChange(it)
            },
            modifier = Modifier.padding(start = Spacing.regular),
        )
    }
}
