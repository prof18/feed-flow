package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.Dialog
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@OptIn(ExperimentalJewelApi::class)
@Composable
internal fun DesktopFeedMoveDialog(
    categories: ImmutableList<FeedSourceCategory>,
    feedSources: ImmutableList<FeedSource>,
    onMove: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val targets = (listOf(null) + categories).map(::DesktopFeedMoveDestination)
    var selectedId by remember { mutableStateOf(DesktopDrawerUncategorizedSectionKey) }
    val selectedIndex = targets.indexOfFirst { it.id == selectedId }
    val selectedCategory = targets.getOrNull(selectedIndex)?.category
    val sourcesToMove = feedSources.filter { it.category?.id != selectedCategory?.id }
    Dialog(title = strings.changeCategory, onCloseRequest = onDismiss) {
        val destinationFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { destinationFocus.requestFocus() }
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.desktopFeedSelectionCount(feedSources.size))
            ListComboBox(
                modifier = Modifier.focusRequester(destinationFocus),
                items = targets,
                selectedIndex = selectedIndex,
                onSelectedItemChange = { index -> targets.getOrNull(index)?.let { selectedId = it.id } },
                itemKeys = { _, destination -> destination.id },
                itemContent = { destination, _, _ -> Text(destination.category?.title ?: strings.noCategory) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) { Text(strings.cancelButton) }
                DefaultButton(
                    onClick = { onMove(sourcesToMove, selectedCategory) },
                    enabled = selectedIndex >= 0 && sourcesToMove.isNotEmpty(),
                ) { Text(strings.confirmButton) }
            }
        }
    }
}

private data class DesktopFeedMoveDestination(val category: FeedSourceCategory?) {
    val id: String get() = category?.id ?: DesktopDrawerUncategorizedSectionKey
}
