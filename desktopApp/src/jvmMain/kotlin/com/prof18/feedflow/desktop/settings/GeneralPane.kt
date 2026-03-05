package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun GeneralPane(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    isMultiPaneEnabled: Boolean,
    onMultiPaneToggled: (Boolean) -> Unit,
    isReduceMotionEnabled: Boolean,
    onReduceMotionToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        val themeModeLabel = when (themeMode) {
            ThemeMode.LIGHT -> strings.settingsThemeLight
            ThemeMode.DARK -> strings.settingsThemeDark
            ThemeMode.OLED -> strings.settingsThemeOled
            ThemeMode.SYSTEM -> strings.settingsThemeSystem
        }

        SettingSelectorItem(
            title = strings.settingsTheme,
            currentValueLabel = themeModeLabel,
            icon = Icons.Outlined.DarkMode,
            onClick = { showThemeDialog = true },
        )

        SettingSwitchItem(
            title = strings.settingsDesktopMultiPaneLayout,
            icon = Icons.Outlined.ViewColumn,
            isChecked = isMultiPaneEnabled,
            onCheckedChange = onMultiPaneToggled,
        )

        SettingSwitchItem(
            title = strings.settingsReduceMotion,
            icon = Icons.Outlined.Animation,
            isChecked = isReduceMotionEnabled,
            onCheckedChange = onReduceMotionToggled,
        )
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            currentThemeMode = themeMode,
            onThemeModeSelected = { selected ->
                onThemeModeSelected(selected)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}

@Composable
private fun ThemeModeDialog(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val modes = listOf(
        ThemeMode.SYSTEM to strings.settingsThemeSystem,
        ThemeMode.LIGHT to strings.settingsThemeLight,
        ThemeMode.DARK to strings.settingsThemeDark,
        ThemeMode.OLED to strings.settingsThemeOled,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingsTheme) },
        text = {
            Column {
                modes.forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentThemeMode == mode,
                                onClick = { onThemeModeSelected(mode) },
                            )
                            .padding(vertical = Spacing.small),
                    ) {
                        RadioButton(
                            selected = currentThemeMode == mode,
                            onClick = { onThemeModeSelected(mode) },
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = Spacing.small),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelButton)
            }
        },
    )
}

@Preview
@Composable
private fun GeneralPanePreview() {
    FeedFlowTheme {
        GeneralPane(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeSelected = {},
            isMultiPaneEnabled = false,
            onMultiPaneToggled = {},
            isReduceMotionEnabled = false,
            onReduceMotionToggled = {},
        )
    }
}
