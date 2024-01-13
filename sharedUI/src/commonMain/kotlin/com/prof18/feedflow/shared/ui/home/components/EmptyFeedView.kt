package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.MR
import com.prof18.feedflow.shared.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun EmptyFeedView(
    modifier: Modifier = Modifier,
    onReloadClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(resource = MR.strings.empty_feed_message),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = {
                onReloadClick()
            },
        ) {
            Text(
                stringResource(resource = MR.strings.refresh_feeds),
            )
        }
    }
}
