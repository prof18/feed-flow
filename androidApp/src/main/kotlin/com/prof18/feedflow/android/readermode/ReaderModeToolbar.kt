package com.prof18.feedflow.android.readermode

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun ReaderModeToolbar(
    navigateBack: () -> Unit,
    isDetailFullscreen: Boolean = false,
    onToggleDetailFullscreen: (() -> Unit)? = null,
) {
    val backButtonColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        TopAppBar(
            title = {},
            navigationIcon = {
                FilledIconButton(
                    onClick = onToggleDetailFullscreen ?: navigateBack,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag(ReaderModeE2eIds.BACK_BUTTON),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = backButtonColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = if (onToggleDetailFullscreen != null && !isDetailFullscreen) {
                            Icons.Default.Fullscreen
                        } else {
                            Icons.AutoMirrored.Filled.ArrowBack
                        },
                        contentDescription = null,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        )
    }
}
