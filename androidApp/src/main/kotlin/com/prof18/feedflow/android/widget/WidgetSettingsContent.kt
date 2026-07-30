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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.prof18.feedflow.android.settings.SettingsE2eIds
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsContent(
    settingsState: WidgetSettingsState,
    onFeedLayoutSelected: (WidgetFeedLayout) -> Unit,
    onShowHeaderSelected: (Boolean) -> Unit,
    onFontScaleSelected: (Int) -> Unit,
    onBackgroundColorSelected: (Int?) -> Unit,
    onBackgroundOpacitySelected: (Int) -> Unit,
    onTextColorModeSelected: (WidgetTextColorMode) -> Unit,
    onHideImagesSelected: (Boolean) -> Unit,
    onCardSurfaceColorSelected: (Int?) -> Unit,
    onCardSurfaceOpacitySelected: (Int) -> Unit,
    onCardCornerRadiusSelected: (Int) -> Unit,
    onCardItemSeparationSelected: (WidgetCardItemSeparation) -> Unit,
    onCardDividerOpacitySelected: (Int) -> Unit,
    onCardImageSizingSelected: (WidgetCardImageSizing) -> Unit,
    showConfirmButton: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val backgroundOpacity = settingsState.backgroundOpacityPercent.coerceIn(minimumValue = 0, maximumValue = 100)
    val defaultBackgroundColor = MaterialTheme.colorScheme.surface
    val resolvedBackgroundColor = settingsState.backgroundColor?.let(::widgetColorFromArgb) ?: defaultBackgroundColor
    val backgroundLabel = settingsState.backgroundColor?.let(::formatWidgetColorHex)
        ?: strings.widgetBackgroundColorDefault
    var showBackgroundColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = Spacing.regular),
            text = strings.widgetConfigurationDescription,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (settingsState.syncPeriod == SyncPeriod.NEVER) {
            Text(
                modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
                text = strings.widgetBackgroundSyncDisabledWarning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(
            text = strings.widgetAppearanceTitle,
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
            style = MaterialTheme.typography.bodyLarge,
        )

        WidgetFeedLayoutSelector(
            feedLayout = settingsState.feedLayout,
            onFeedLayoutSelected = onFeedLayoutSelected,
        )

        SettingSwitchItem(
            title = strings.widgetShowHeaderTitle,
            isChecked = settingsState.showHeader,
            onCheckedChange = onShowHeaderSelected,
        )

        SettingSwitchItem(
            title = strings.settingsHideImages,
            isChecked = settingsState.hideImages,
            onCheckedChange = onHideImagesSelected,
        )

        if (settingsState.feedLayout == WidgetFeedLayout.CARD) {
            WidgetCardAppearanceSettings(
                settingsState = settingsState,
                onSurfaceColorSelected = onCardSurfaceColorSelected,
                onSurfaceOpacitySelected = onCardSurfaceOpacitySelected,
                onCornerRadiusSelected = onCardCornerRadiusSelected,
                onItemSeparationSelected = onCardItemSeparationSelected,
                onDividerOpacitySelected = onCardDividerOpacitySelected,
                onImageSizingSelected = onCardImageSizingSelected,
            )
        }

        WidgetColorSettingItem(
            title = strings.widgetBackgroundColorTitle,
            currentValueLabel = backgroundLabel,
            color = resolvedBackgroundColor,
            onClick = { showBackgroundColorPicker = true },
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

        WidgetTextColorSelector(
            currentMode = settingsState.textColorMode,
            onModeSelected = onTextColorModeSelected,
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

    if (showBackgroundColorPicker) {
        WidgetColorPickerDialog(
            initialColor = resolvedBackgroundColor,
            labels = WidgetColorPickerLabels(
                title = strings.widgetBackgroundColorTitle,
                preview = strings.widgetBackgroundColorPreview,
                brightness = strings.widgetBackgroundColorBrightness,
                hexLabel = strings.widgetBackgroundColorHexLabel,
                hexHint = strings.widgetBackgroundColorHexHint,
                hexError = strings.widgetBackgroundColorHexError,
                resetToDefault = strings.widgetBackgroundColorReset,
            ),
            onDismiss = { showBackgroundColorPicker = false },
            onConfirm = { color ->
                onBackgroundColorSelected(widgetColorToOpaqueArgb(color))
                showBackgroundColorPicker = false
            },
            onReset = {
                onBackgroundColorSelected(null)
                showBackgroundColorPicker = false
            },
        )
    }
}

@Composable
private fun WidgetCardAppearanceSettings(
    settingsState: WidgetSettingsState,
    onSurfaceColorSelected: (Int?) -> Unit,
    onSurfaceOpacitySelected: (Int) -> Unit,
    onCornerRadiusSelected: (Int) -> Unit,
    onItemSeparationSelected: (WidgetCardItemSeparation) -> Unit,
    onDividerOpacitySelected: (Int) -> Unit,
    onImageSizingSelected: (WidgetCardImageSizing) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val appearance = settingsState.cardAppearance
    val surfaceOpacity = appearance.surfaceOpacityPercent.coerceIn(minimumValue = 0, maximumValue = 100)
    val cornerRadius = appearance.cornerRadiusDp.coerceIn(minimumValue = 0, maximumValue = 32)
    val dividerOpacity = appearance.dividerOpacityPercent.coerceIn(minimumValue = 0, maximumValue = 100)
    val themedCardColor = MaterialTheme.colorScheme.secondaryContainer
    val resolvedCardColor = appearance.surfaceColor?.let(::widgetColorFromArgb) ?: themedCardColor
    val cardColorLabel = appearance.surfaceColor?.let(::formatWidgetColorHex)
        ?: strings.widgetCardSurfaceColorDefault
    var showCardColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.testTag(SettingsE2eIds.WIDGET_CARD_APPEARANCE),
    ) {
        Text(
            text = strings.widgetCardAppearanceTitle,
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
            style = MaterialTheme.typography.bodyLarge,
        )

        WidgetColorSettingItem(
            title = strings.widgetCardSurfaceColorTitle,
            currentValueLabel = cardColorLabel,
            color = resolvedCardColor,
            onClick = { showCardColorPicker = true },
            modifier = Modifier.testTag(SettingsE2eIds.WIDGET_CARD_SURFACE_COLOR),
        )

        Text(
            text = strings.widgetCardSurfaceOpacityTitle(surfaceOpacity.toString()),
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            modifier = Modifier
                .padding(horizontal = Spacing.regular)
                .testTag(SettingsE2eIds.WIDGET_CARD_SURFACE_OPACITY),
            value = surfaceOpacity.toFloat(),
            onValueChange = { onSurfaceOpacitySelected(it.roundToInt()) },
            valueRange = 0f..100f,
        )

        Text(
            text = strings.widgetCardCornerRadiusTitle(cornerRadius.toString()),
            modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            modifier = Modifier
                .padding(horizontal = Spacing.regular)
                .testTag(SettingsE2eIds.WIDGET_CARD_CORNER_RADIUS),
            value = cornerRadius.toFloat(),
            onValueChange = { onCornerRadiusSelected(it.roundToInt()) },
            valueRange = 0f..32f,
            steps = 15,
        )

        CompactSettingDropdownRow(
            title = strings.widgetCardItemSeparationTitle,
            currentValue = appearance.itemSeparation,
            options = persistentListOf(
                SettingDropdownOption(
                    WidgetCardItemSeparation.SPACING,
                    strings.widgetCardItemSeparationSpacing,
                    e2eId = SettingsE2eIds.WIDGET_CARD_ITEM_SEPARATION_SPACING,
                ),
                SettingDropdownOption(
                    WidgetCardItemSeparation.DIVIDER,
                    strings.widgetCardItemSeparationDivider,
                    e2eId = SettingsE2eIds.WIDGET_CARD_ITEM_SEPARATION_DIVIDER,
                ),
                SettingDropdownOption(
                    WidgetCardItemSeparation.NONE,
                    strings.widgetCardItemSeparationNone,
                    e2eId = SettingsE2eIds.WIDGET_CARD_ITEM_SEPARATION_NONE,
                ),
            ),
            onOptionSelected = onItemSeparationSelected,
            modifier = Modifier.testTag(SettingsE2eIds.WIDGET_CARD_ITEM_SEPARATION),
        )

        if (appearance.itemSeparation == WidgetCardItemSeparation.DIVIDER) {
            Text(
                text = strings.widgetCardDividerOpacityTitle(dividerOpacity.toString()),
                modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .testTag(SettingsE2eIds.WIDGET_CARD_DIVIDER_OPACITY),
                value = dividerOpacity.toFloat(),
                onValueChange = { onDividerOpacitySelected(it.roundToInt()) },
                valueRange = 0f..100f,
            )
        }

        if (!settingsState.hideImages) {
            CompactSettingDropdownRow(
                title = strings.widgetCardImageSizingTitle,
                currentValue = appearance.imageSizing,
                options = persistentListOf(
                    SettingDropdownOption(
                        WidgetCardImageSizing.THUMBNAIL,
                        strings.widgetCardImageSizingThumbnail,
                        e2eId = SettingsE2eIds.WIDGET_CARD_IMAGE_THUMBNAIL,
                    ),
                    SettingDropdownOption(
                        WidgetCardImageSizing.FILL_ROW_HEIGHT,
                        strings.widgetCardImageSizingFillRowHeight,
                        e2eId = SettingsE2eIds.WIDGET_CARD_IMAGE_FILL,
                    ),
                ),
                onOptionSelected = onImageSizingSelected,
                modifier = Modifier.testTag(SettingsE2eIds.WIDGET_CARD_IMAGE_SIZING),
            )
        }

        if (showCardColorPicker) {
            WidgetColorPickerDialog(
                initialColor = resolvedCardColor,
                labels = WidgetColorPickerLabels(
                    title = strings.widgetCardSurfaceColorTitle,
                    preview = strings.widgetCardSurfaceColorPreview,
                    brightness = strings.widgetCardSurfaceColorBrightness,
                    hexLabel = strings.widgetCardSurfaceColorHexLabel,
                    hexHint = strings.widgetCardSurfaceColorHexHint,
                    hexError = strings.widgetCardSurfaceColorHexError,
                    resetToDefault = strings.widgetCardSurfaceColorReset,
                ),
                onDismiss = { showCardColorPicker = false },
                onConfirm = { color ->
                    onSurfaceColorSelected(widgetColorToOpaqueArgb(color))
                    showCardColorPicker = false
                },
                onReset = {
                    onSurfaceColorSelected(null)
                    showCardColorPicker = false
                },
            )
        }
    }
}

@Composable
private fun WidgetFeedLayoutSelector(
    feedLayout: WidgetFeedLayout,
    onFeedLayoutSelected: (WidgetFeedLayout) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    CompactSettingDropdownRow(
        title = strings.feedLayoutTitle,
        currentValue = feedLayout,
        options = persistentListOf(
            SettingDropdownOption(
                WidgetFeedLayout.LIST,
                strings.settingsFeedLayoutList,
                e2eId = SettingsE2eIds.WIDGET_FEED_LAYOUT_LIST,
            ),
            SettingDropdownOption(
                WidgetFeedLayout.CARD,
                strings.settingsFeedLayoutCard,
                e2eId = SettingsE2eIds.WIDGET_FEED_LAYOUT_CARD,
            ),
        ),
        onOptionSelected = onFeedLayoutSelected,
        modifier = Modifier.testTag(SettingsE2eIds.WIDGET_FEED_LAYOUT),
    )
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
    labels: WidgetColorPickerLabels,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    onReset: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val controller = rememberColorPickerController()
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }
    var hexInput by remember(initialColor) {
        mutableStateOf(
            formatWidgetColorHex(widgetColorToOpaqueArgb(initialColor)),
        )
    }
    val parsedHexColor = parseWidgetColorHex(hexInput)
    val isHexInputValid = parsedHexColor != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .padding(Spacing.regular)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(
                    text = labels.title,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
                ) {
                    Text(
                        text = labels.preview,
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
                        hexInput = formatWidgetColorHex(widgetColorToOpaqueArgb(colorEnvelope.color))
                    },
                )

                Text(
                    text = labels.brightness,
                    style = MaterialTheme.typography.bodyMedium,
                )

                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    controller = controller,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    initialColor = initialColor,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = hexInput,
                    onValueChange = { value ->
                        val updatedValue = value.uppercase()
                        hexInput = updatedValue
                        parseWidgetColorHex(updatedValue)?.let { color ->
                            controller.selectByColor(color = color, fromUser = false)
                        }
                    },
                    label = {
                        Text(text = labels.hexLabel)
                    },
                    singleLine = true,
                    isError = hexInput.isNotBlank() && !isHexInputValid,
                    supportingText = {
                        val supportingText = if (hexInput.isBlank() || isHexInputValid) {
                            labels.hexHint
                        } else {
                            labels.hexError
                        }
                        Text(text = supportingText)
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = {
                            onReset()
                        },
                    ) {
                        Text(text = labels.resetToDefault)
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = strings.cancelButton)
                        }
                        TextButton(
                            onClick = { onConfirm(selectedColor) },
                            enabled = isHexInputValid,
                        ) {
                            Text(text = strings.confirmButton)
                        }
                    }
                }
            }
        }
    }
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
                    feedLayout = WidgetFeedLayout.CARD,
                    showHeader = true,
                    fontScale = 0,
                    backgroundColor = null,
                    backgroundOpacityPercent = 80,
                ),
                onFeedLayoutSelected = {},
                onShowHeaderSelected = {},
                onFontScaleSelected = {},
                onBackgroundColorSelected = {},
                onBackgroundOpacitySelected = {},
                onTextColorModeSelected = {},
                onHideImagesSelected = {},
                onCardSurfaceColorSelected = {},
                onCardSurfaceOpacitySelected = {},
                onCardCornerRadiusSelected = {},
                onCardItemSeparationSelected = {},
                onCardDividerOpacitySelected = {},
                onCardImageSizingSelected = {},
                showConfirmButton = true,
                onConfirm = {},
            )
        }
    }
}

private data class WidgetColorPickerLabels(
    val title: String,
    val preview: String,
    val brightness: String,
    val hexLabel: String,
    val hexHint: String,
    val hexError: String,
    val resetToDefault: String,
)
