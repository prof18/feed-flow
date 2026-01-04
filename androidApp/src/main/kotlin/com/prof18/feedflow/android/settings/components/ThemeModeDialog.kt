package com.prof18.feedflow.android.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun ThemeModeDialog(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    dismissDialog: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    Dialog(
        onDismissRequest = dismissDialog,
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.regular),
        ) {
            Text(
                text = strings.settingsTheme,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .padding(bottom = Spacing.regular),
            )

            ThemeModeOption(
                text = strings.settingsThemeSystem,
                selected = currentThemeMode == ThemeMode.SYSTEM,
                onClick = {
                    onThemeModeSelected(ThemeMode.SYSTEM)
                },
            )
            ThemeModeOption(
                text = strings.settingsThemeLight,
                selected = currentThemeMode == ThemeMode.LIGHT,
                onClick = {
                    onThemeModeSelected(ThemeMode.LIGHT)
                },
            )
            ThemeModeOption(
                text = strings.settingsThemeDark,
                selected = currentThemeMode == ThemeMode.DARK,
                onClick = {
                    onThemeModeSelected(ThemeMode.DARK)
                },
            )
            ThemeModeOption(
                text = strings.settingsThemeOled,
                selected = currentThemeMode == ThemeMode.OLED,
                onClick = {
                    onThemeModeSelected(ThemeMode.OLED)
                },
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.small),
        )
    }
}

@Preview
@Composable
private fun ThemeModeDialogPreview() {
    FeedFlowTheme {
        ThemeModeDialog(
            currentThemeMode = ThemeMode.SYSTEM,
            onThemeModeSelected = {},
            dismissDialog = {},
        )
    }
}
