package com.prof18.feedflow.desktop.ui.settings.blockedwords

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.presentation.BlockedWordsViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.koinInject // Common Koin utility for Compose

// Assuming DesktopViewModel is similar to how other desktop ViewModels are accessed
// For now, using koinInject as a placeholder for ViewModel retrieval.
// This might need to be adjusted based on the project's specific desktop Koin setup.

@Composable
fun BlockedWordsScreen(
    onBackClick: () -> Unit,
    // If Koin setup for desktop ViewModels differs, this might need to be passed
    // or obtained via a specific desktop Koin helper.
    // For now, let's assume koinInject can work or is adapted for desktop.
    viewModel: BlockedWordsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar / Title Area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = LocalFeedFlowStrings.current.commonBackCd 
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = LocalFeedFlowStrings.current.blockedWordsTitle, 
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.newWordText,
                onValueChange = { viewModel.updateNewWordText(it) },
                label = { Text(LocalFeedFlowStrings.current.blockedWordsInputPlaceholder) }, 
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { viewModel.addBlockedWord() },
                enabled = uiState.newWordText.isNotBlank()
            ) {
                Text(LocalFeedFlowStrings.current.blockedWordsAddButton) 
            }
        }

        // Error State
        uiState.error?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Loading State or List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f), // Takes remaining space
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (uiState.blockedWords.isEmpty() && uiState.error == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(LocalFeedFlowStrings.current.blockedWordsEmptyList) 
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f) // Takes remaining space
                ) {
                    items(uiState.blockedWords, key = { it }) { word ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = word, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteBlockedWord(word) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = LocalFeedFlowStrings.current.blockedWordsDeleteContentDescription.format(word) 
                                )
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
