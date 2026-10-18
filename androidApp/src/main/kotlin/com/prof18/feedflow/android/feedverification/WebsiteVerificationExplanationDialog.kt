package com.prof18.feedflow.android.feedverification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import java.net.URI

@Composable
internal fun WebsiteVerificationExplanationDialog(
    websiteUrl: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    readerOptionsHint: String? = null,
) {
    val strings = LocalFeedFlowStrings.current
    val keyboard = LocalSoftwareKeyboardController.current
    SideEffect(websiteUrl) { keyboard?.hide() }
    AlertDialog(
        modifier = Modifier.semantics { testTagsAsResourceId = true }
            .testTag(FeedVerificationE2eIds.EXPLANATION),
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text(strings.feedVerificationExplanationTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(verificationWebsiteName(websiteUrl), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                Text(strings.feedVerificationExplanationBody)
                readerOptionsHint?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.testTag(FeedVerificationE2eIds.OPEN)) {
                Text(strings.feedVerificationOpenWebsite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancelButton) }
        },
    )
}

internal fun verificationWebsiteName(url: String): String =
    runCatching { URI(url).host.orEmpty() }.getOrDefault("")
