package com.prof18.feedflow.android.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ViewHeadline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.prof18.feedflow.android.settings.components.SyncPeriodSelector
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsContent(
    settingsState: WidgetSettingsState,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onFeedLayoutSelected: (FeedLayout) -> Unit,
    onShowHeaderSelected: (Boolean) -> Unit,
    onFontScaleSelected: (Int) -> Unit,
    onBackgroundColorSelected: (Int?) -> Unit,
    onBackgroundOpacitySelected: (Int) -> Unit,
    showConfirmButton: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val backgroundOpacity = settingsState.backgroundOpacityPercent.coerceIn(minimumValue = 0, maximumValue = 100)
    val defaultBackgroundColor = MaterialTheme.colorScheme.surface
    val resolvedBackgroundColor = settingsState.backgroundColor?.let { Color(it) } ?: defaultBackgroundColor
    val backgroundLabel = settingsState.backgroundColor?.let { formatColorHex(it) }
        ?: strings.widgetBackgroundColorDefault
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = Spacing.regular),
            text = strings.widgetConfigurationDescription,
            style = MaterialTheme.typography.bodyMedium,
        )

        SyncPeriodSelector(
            currentPeriod = settingsState.syncPeriod,
            onPeriodSelected = onSyncPeriodSelected,
            showNeverSync = false,
        )

        Text(
            text = strings.widgetAppearanceTitle,
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
            style = MaterialTheme.typography.bodyLarge,
        )

        FeedLayoutSelector(
            feedLayout = settingsState.feedLayout,
            onFeedLayoutSelected = onFeedLayoutSelected,
        )

        SettingSwitchItem(
            title = strings.widgetShowHeaderTitle,
            icon = Icons.Outlined.ViewHeadline,
            isChecked = settingsState.showHeader,
            onCheckedChange = onShowHeaderSelected,
        )

        WidgetColorSettingItem(
            title = strings.widgetBackgroundColorTitle,
            currentValueLabel = backgroundLabel,
            color = resolvedBackgroundColor,
            onClick = { showColorPicker = true },
        )

        Text(
            text = strings.widgetBackgroundOpacityTitle(backgroundOpacity.toString()),
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
        )

        Slider(
            modifier = Modifier.padding(horizontal = Spacing.regular),
            value = backgroundOpacity.toFloat(),
            onValueChange = { onBackgroundOpacitySelected(it.roundToInt()) },
            valueRange = 0f..100f,
        )

        Text(
            text = strings.widgetFontSizeTitle,
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
        )

        SliderWithPlusMinus(
            modifier = Modifier.padding(horizontal = Spacing.regular),
            value = settingsState.fontScale.toFloat(),
            onValueChange = { onFontScaleSelected(it.roundToInt()) },
            valueRange = MIN_WIDGET_FONT_SCALE.toFloat()..MAX_WIDGET_FONT_SCALE.toFloat(),
            steps = MAX_WIDGET_FONT_SCALE - MIN_WIDGET_FONT_SCALE,
        )

        if (showConfirmButton) {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .fillMaxWidth()
                    .padding(vertical = Spacing.medium),
            ) {
                Text(text = strings.widgetConfigurationConfirm)
            }
        }
    }

    if (showColorPicker) {
        WidgetColorPickerDialog(
            initialColor = resolvedBackgroundColor,
            defaultColor = defaultBackgroundColor,
            onDismiss = { showColorPicker = false },
            onConfirm = { color ->
                onBackgroundColorSelected(color.toArgb())
                showColorPicker = false
            },
            onReset = {
                onBackgroundColorSelected(null)
                showColorPicker = false
            },
        )
    }
}

@Composable
private fun WidgetColorSettingItem(
    title: String,
    currentValueLabel: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            imageVector = Icons.Outlined.Palette,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = currentValueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.size(Spacing.small))

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(28.dp)
                .background(color = color, shape = shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        )
    }
}

@Composable
private fun WidgetColorPickerDialog(
    initialColor: Color,
    defaultColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    onReset: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val controller = rememberColorPickerController()
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.regular),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(
                    text = strings.widgetBackgroundColorTitle,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
                ) {
                    Text(
                        text = strings.widgetBackgroundColorPreview,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(selectedColor, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    )
                }

                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    controller = controller,
                    initialColor = initialColor,
                    onColorChanged = { colorEnvelope ->
                        selectedColor = colorEnvelope.color
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = {
                            controller.selectByColor(defaultColor, false)
                            selectedColor = defaultColor
                            onReset()
                        },
                    ) {
                        Text(text = strings.resetButton)
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = strings.cancelButton)
                        }
                        TextButton(onClick = { onConfirm(selectedColor) }) {
                            Text(text = strings.confirmButton)
                        }
                    }
                }
            }
        }
    }
}

private fun formatColorHex(colorArgb: Int): String {
    return String.format(Locale.US, "#%08X", colorArgb)
}

@Preview
@Composable
private fun WidgetSettingsContentPreview() {
    FeedFlowTheme {
        Surface(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        ) {
            WidgetSettingsContent(
                settingsState = WidgetSettingsState(
                    syncPeriod = SyncPeriod.ONE_HOUR,
                    feedLayout = FeedLayout.CARD,
                    showHeader = true,
                    fontScale = 0,
                    backgroundColor = null,
                    backgroundOpacityPercent = 80,
                ),
                onSyncPeriodSelected = {},
                onFeedLayoutSelected = {},
                onShowHeaderSelected = {},
                onFontScaleSelected = {},
                onBackgroundColorSelected = {},
                onBackgroundOpacitySelected = {},
                showConfirmButton = true,
                onConfirm = {},
            )
        }
    }
}
