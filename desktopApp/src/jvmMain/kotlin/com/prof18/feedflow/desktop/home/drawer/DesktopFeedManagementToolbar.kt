package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.rememberWrappingOutlinedButtonStyle
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun DesktopFeedManagementToolbar(
    selectedCount: Int,
    onClear: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val wrappingStyle = rememberWrappingOutlinedButtonStyle()
    Column(
        modifier = modifier.padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(strings.desktopFeedSelectionCount(selectedCount))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onClear,
                enabled = selectedCount > 0,
                style = wrappingStyle,
                modifier = Modifier.heightIn(min = 36.dp),
            ) { Text(strings.desktopClearFeedSelection) }
            OutlinedButton(
                onClick = onMove,
                enabled = selectedCount > 0,
                style = wrappingStyle,
                modifier = Modifier.heightIn(min = 36.dp),
            ) { Text(strings.desktopMoveSelectedFeeds) }
            OutlinedButton(
                onClick = onDelete,
                enabled = selectedCount > 0,
                style = wrappingStyle,
                modifier = Modifier.heightIn(min = 36.dp),
            ) { Text(strings.desktopDeleteSelectedFeeds) }
        }
    }
}
