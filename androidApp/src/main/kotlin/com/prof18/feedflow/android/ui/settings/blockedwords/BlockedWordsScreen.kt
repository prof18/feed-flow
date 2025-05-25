package com.prof18.feedflow.android.ui.settings.blockedwords

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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedWordsScreen(
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<BlockedWordsViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.blockedWordsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = LocalFeedFlowStrings.current.commonBackCd
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Input Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newWordText,
                    onValueChange = { viewModel.updateNewWordText(it) },
                    label = { Text(LocalFeedFlowStrings.current.blockedWordsInputPlaceholder) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.addBlockedWord() },
                    enabled = uiState.newWordText.isNotBlank()
                ) {
                    Text(LocalFeedFlowStrings.current.blockedWordsAddButton)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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

            // Display Area for Blocked Words
            if (!uiState.isLoading && uiState.blockedWords.isEmpty() && uiState.error == null) {
                 Text(
                    text = LocalFeedFlowStrings.current.blockedWordsEmptyList,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else if (!uiState.isLoading || uiState.blockedWords.isNotEmpty()) { // Show list if not loading OR if list is not empty (even if loading more/error)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
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
