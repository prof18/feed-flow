package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun BlockedWordsContent(
    keywords: ImmutableList<String>,
    onAddWord: (String) -> Unit,
    onRemoveWord: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    Column(
        modifier = modifier.padding(Spacing.regular),
        verticalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Text(
            text = LocalFeedFlowStrings.current.blockedWordsDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AddWordInput(onAddWord = onAddWord)

        if (keywords.isEmpty()) {
            Text(
                text = LocalFeedFlowStrings.current.blockedWordsEmptyState,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.regular),
            )
        } else {
            LazyColumn {
                items(keywords.size) { index ->
                    WordItem(
                        word = keywords[index],
                        onRemove = { onRemoveWord(keywords[index]) },
                        showDivider = index < keywords.size - 1,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(bottomPadding))
                }
            }
        }
    }
}

@Composable
private fun AddWordInput(
    onAddWord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var wordText by remember { mutableStateOf("") }

    val addWord = {
        val trimmedText = wordText.trim()
        if (trimmedText.isNotEmpty()) {
            onAddWord(trimmedText)
            wordText = ""
        }
    }

    OutlinedTextField(
        value = wordText,
        onValueChange = { wordText = it },
        placeholder = {
            Text(
                text = LocalFeedFlowStrings.current.addWordPlaceholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (wordText.trim().isNotEmpty()) {
                IconButton(onClick = addWord) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.None,
        ),
        keyboardActions = KeyboardActions(
            onDone = { addWord() },
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun WordItem(
    word: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (showDivider) {
            HorizontalDivider()
        }
    }
}

@Preview
@Composable
private fun BlockedKeywordsEmptyPreview() {
    BlockedWordsContent(
        keywords = emptyList<String>().toImmutableList(),
        onAddWord = {},
        onRemoveWord = {},
    )
}

@Preview
@Composable
private fun BlockedKeywordsPopulatedPreview() {
    BlockedWordsContent(
        keywords = listOf("politics", "crypto", "sports", "another", "test").toImmutableList(),
        onAddWord = {},
        onRemoveWord = {},
    )
}
