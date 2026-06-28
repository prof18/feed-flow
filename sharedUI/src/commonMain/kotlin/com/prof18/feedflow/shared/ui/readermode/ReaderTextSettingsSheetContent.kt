package com.prof18.feedflow.shared.ui.readermode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

private const val FONT_SIZE_VALUE_LABEL_TAG = "reader_font_size_value_label"
private const val LINE_HEIGHT_VALUE_LABEL_TAG = "reader_line_height_value_label"
private const val MIN_FONT_SIZE = 12f
private const val MAX_FONT_SIZE = 40f
private const val FONT_SIZE_STEPS = 27
private const val MIN_LINE_HEIGHT = 0f
private const val MAX_LINE_HEIGHT = 15f
private const val LINE_HEIGHT_STEPS = 14
private const val LINE_HEIGHT_BASE_TENTHS = 15
private const val LINE_HEIGHT_TENTHS_DENOMINATOR = 10

@Composable
fun ReaderTextSettingsSheetContent(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    lineHeight: Int,
    onLineHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    resetButtonModifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    Column(
        modifier = modifier.padding(Spacing.regular),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = strings.readerModeTextSettings,
            style = MaterialTheme.typography.titleMedium,
        )

        ReaderTextSettingSliderRow(
            title = strings.readerModeFontSize,
            valueLabel = fontSize.toString(),
            valueLabelTestTag = FONT_SIZE_VALUE_LABEL_TAG,
            value = fontSize.toFloat(),
            onValueChange = { onFontSizeChange(it.toInt()) },
            valueRange = MIN_FONT_SIZE..MAX_FONT_SIZE,
            steps = FONT_SIZE_STEPS,
        )

        ReaderTextSettingSliderRow(
            title = strings.readerModeLineHeight,
            valueLabel = readerLineHeightValueLabel(lineHeight),
            valueLabelTestTag = LINE_HEIGHT_VALUE_LABEL_TAG,
            value = lineHeight.toFloat(),
            onValueChange = { onLineHeightChange(it.toInt()) },
            valueRange = MIN_LINE_HEIGHT..MAX_LINE_HEIGHT,
            steps = LINE_HEIGHT_STEPS,
        )

        TextButton(
            onClick = {
                onFontSizeChange(ReaderModeDefaults.FONT_SIZE)
                onLineHeightChange(ReaderModeDefaults.LINE_HEIGHT)
            },
            enabled = fontSize != ReaderModeDefaults.FONT_SIZE ||
                lineHeight != ReaderModeDefaults.LINE_HEIGHT,
            modifier = resetButtonModifier.align(Alignment.End),
        ) {
            Text(strings.readerModeResetToDefault)
        }
    }
}

@Composable
private fun ReaderTextSettingSliderRow(
    title: String,
    valueLabel: String,
    valueLabelTestTag: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xsmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = valueLabel,
                modifier = Modifier.testTag(valueLabelTestTag),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SliderWithoutButtons(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

private fun readerLineHeightValueLabel(lineHeight: Int): String {
    val tenths = LINE_HEIGHT_BASE_TENTHS + lineHeight
    return "${tenths / LINE_HEIGHT_TENTHS_DENOMINATOR}.${tenths % LINE_HEIGHT_TENTHS_DENOMINATOR}"
}
