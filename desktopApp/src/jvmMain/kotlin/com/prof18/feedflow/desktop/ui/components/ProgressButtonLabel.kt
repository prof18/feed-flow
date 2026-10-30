package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text

/**
 * Button text that stays centered; while [busy] a spinner takes its place without changing the button width.
 */
@Composable
internal fun ProgressButtonLabel(text: String, busy: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Text(text, Modifier.alpha(if (busy) 0f else 1f))
        if (busy) CircularProgressIndicator(Modifier.size(14.dp))
    }
}
