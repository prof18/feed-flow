package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AudioEpisodeBanner(
    title: String?,
    fontSize: Int,
    onOpenAudio: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val episodeTitle = title?.takeIf { it.isNotBlank() } ?: strings.audioEpisodeUntitled
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.regular),
        verticalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Text(
            text = episodeTitle,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = (fontSize + 20).sp,
                lineHeight = (fontSize + 32).sp,
            ),
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            onClick = onOpenAudio,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
                .testTag("reader_audio_banner")
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "${strings.menuOpenAudio}: $episodeTitle"
                },
        ) {
            Row(
                modifier = Modifier.padding(Spacing.regular),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    Text(strings.audioEpisodeLabel, style = MaterialTheme.typography.labelMedium)
                    Text(episodeTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        strings.menuOpenAudio,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
