package com.prof18.feedflow.android.readlater

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.core.model.ReadLaterMarkerWithDetails
import com.prof18.feedflow.shared.presentation.ReadLaterViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object ReadLaterE2eIds {
    const val LIST = "read_later_list"
    const val EMPTY = "read_later_empty"
    const val DELETE_BUTTON = "read_later_delete_button"
    fun item(id: String) = "read_later_item_$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadLaterListScreen(
    navigateBack: () -> Unit,
    onMarkerClick: (ReadLaterMarkerWithDetails) -> Unit,
    viewModel: ReadLaterViewModel = koinViewModel(),
) {
    val markers by viewModel.readLaterMarkers.collectAsStateWithLifecycle()
    val strings = LocalFeedFlowStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.readLaterListTitle) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (markers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(ReadLaterE2eIds.EMPTY),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.readLaterEmpty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(ReadLaterE2eIds.LIST),
            ) {
                items(markers, key = { it.id }) { marker ->
                    ReadLaterMarkerItem(
                        marker = marker,
                        onClick = { onMarkerClick(marker) },
                        onDelete = { viewModel.deleteMarker(marker.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadLaterMarkerItem(
    marker: ReadLaterMarkerWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.readLaterRemoveDialogTitle) },
            text = { Text(strings.readLaterRemoveDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                ) {
                    Text(strings.confirmButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancelButton)
                }
            },
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(ReadLaterE2eIds.item(marker.id)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = marker.title ?: marker.url,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = marker.feedSourceTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = formatDate(marker.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            IconButton(
                modifier = Modifier.testTag(ReadLaterE2eIds.DELETE_BUTTON),
                onClick = { showDeleteDialog = true },
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = strings.readLaterRemove,
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(epochMillis))
    } catch (_: Exception) {
        ""
    }
}
