package com.prof18.feedflow.android.ui.readermode

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.core.model.ReaderModeDefaults

private const val FONT_SIZE_VALUE_LABEL_TAG = "reader_font_size_value_label"
private const val LINE_HEIGHT_VALUE_LABEL_TAG = "reader_line_height_value_label"
private const val TITLE_FONT_SIZE_VALUE_LABEL_TAG = "reader_title_font_size_value_label"
private const val FONT_OPTION_TAG_PREFIX = "reader_font_option_"
private const val MIN_FONT_SIZE = 12f
private const val MAX_FONT_SIZE = 40f
private const val FONT_SIZE_STEPS = 27
private const val MIN_LINE_HEIGHT = 0f
private const val MAX_LINE_HEIGHT = 15f
private const val LINE_HEIGHT_STEPS = 14
private const val MIN_TITLE_FONT_SIZE = 16f
private const val MAX_TITLE_FONT_SIZE = 48f
private const val TITLE_FONT_SIZE_STEPS = 31
private const val LINE_HEIGHT_BASE_TENTHS = 15
private const val LINE_HEIGHT_TENTHS_DENOMINATOR = 10
private const val PREVIEW_FADE_START = 0.75f
private const val PREVIEW_TITLE_GAP_RATIO = 0.35f

@Composable
fun ReaderTextSettingsSheetContent(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    lineHeight: Int,
    onLineHeightChange: (Int) -> Unit,
    titleFontSize: Int,
    onTitleFontSizeChange: (Int) -> Unit,
    headlineFont: ReaderFontFamily,
    bodyFont: ReaderFontFamily,
    onFontFamiliesChange: (headline: ReaderFontFamily, body: ReaderFontFamily) -> Unit,
    modifier: Modifier = Modifier,
    resetButtonModifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.regular),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.medium, end = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.readerModeTextSettings,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    onFontSizeChange(ReaderModeDefaults.FONT_SIZE)
                    onLineHeightChange(ReaderModeDefaults.LINE_HEIGHT)
                    onTitleFontSizeChange(ReaderModeDefaults.TITLE_FONT_SIZE)
                    onFontFamiliesChange(ReaderModeDefaults.HEADLINE_FONT, ReaderModeDefaults.BODY_FONT)
                },
                enabled = fontSize != ReaderModeDefaults.FONT_SIZE ||
                    lineHeight != ReaderModeDefaults.LINE_HEIGHT ||
                    titleFontSize != ReaderModeDefaults.TITLE_FONT_SIZE ||
                    headlineFont != ReaderModeDefaults.HEADLINE_FONT ||
                    bodyFont != ReaderModeDefaults.BODY_FONT,
                modifier = resetButtonModifier,
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Text(
                    text = strings.readerModeResetToDefault,
                    modifier = Modifier.padding(start = Spacing.small),
                )
            }
        }

        ReaderTextSettingsPreview(
            fontSize = fontSize,
            lineHeight = lineHeight,
            titleFontSize = titleFontSize,
            headlineFont = headlineFont,
            bodyFont = bodyFont,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        ReaderFontFamilyTileRow(
            title = strings.readerModeFont,
            selected = bodyFont,
            onSelected = { onFontFamiliesChange(it, it) },
        )

        Surface(
            modifier = Modifier.padding(horizontal = Spacing.regular),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                ReaderTextSettingSliderRow(
                    title = strings.readerModeFontSize,
                    valueLabel = fontSize.toString(),
                    valueLabelTestTag = FONT_SIZE_VALUE_LABEL_TAG,
                    value = fontSize.toFloat(),
                    onValueChange = { onFontSizeChange(it.toInt()) },
                    valueRange = MIN_FONT_SIZE..MAX_FONT_SIZE,
                    steps = FONT_SIZE_STEPS,
                    minIcon = Icons.Rounded.TextDecrease,
                    maxIcon = Icons.Rounded.TextIncrease,
                )
                SliderDivider()
                ReaderTextSettingSliderRow(
                    title = strings.readerModeTitleFontSize,
                    valueLabel = titleFontSize.toString(),
                    valueLabelTestTag = TITLE_FONT_SIZE_VALUE_LABEL_TAG,
                    value = titleFontSize.toFloat(),
                    onValueChange = { onTitleFontSizeChange(it.toInt()) },
                    valueRange = MIN_TITLE_FONT_SIZE..MAX_TITLE_FONT_SIZE,
                    steps = TITLE_FONT_SIZE_STEPS,
                    minIcon = Icons.Rounded.TextDecrease,
                    maxIcon = Icons.Rounded.TextIncrease,
                )
                SliderDivider()
                ReaderTextSettingSliderRow(
                    title = strings.readerModeLineHeight,
                    valueLabel = readerLineHeightValueLabel(lineHeight),
                    valueLabelTestTag = LINE_HEIGHT_VALUE_LABEL_TAG,
                    value = lineHeight.toFloat(),
                    onValueChange = { onLineHeightChange(it.toInt()) },
                    valueRange = MIN_LINE_HEIGHT..MAX_LINE_HEIGHT,
                    steps = LINE_HEIGHT_STEPS,
                    minIcon = Icons.Rounded.FormatLineSpacing,
                    maxIcon = Icons.Rounded.FormatLineSpacing,
                )
            }
        }
    }
}

// Fixed height, so dragging a size slider never moves the slider under the finger.
@Composable
private fun ReaderTextSettingsPreview(
    fontSize: Int,
    lineHeight: Int,
    titleFontSize: Int,
    headlineFont: ReaderFontFamily,
    bodyFont: ReaderFontFamily,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clearAndSetSemantics {},
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            PREVIEW_FADE_START to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy((titleFontSize * PREVIEW_TITLE_GAP_RATIO).dp),
        ) {
            Text(
                text = strings.readerModePreviewTitle,
                fontFamily = rememberReaderFontFamily(headlineFont),
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize.sp,
                lineHeight = 1.2.em,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = previewTitleMaxLines(titleFontSize),
                overflow = TextOverflow.Ellipsis,
                style = LocalTextStyle.current.copy(lineHeightStyle = PreviewLineHeightStyle),
            )
            Text(
                text = strings.readerModePreviewBody,
                fontFamily = rememberReaderFontFamily(bodyFont),
                fontSize = fontSize.sp,
                // Same formula as the reader page: line-height 1.5 + step / 10.
                lineHeight = ((LINE_HEIGHT_BASE_TENTHS + lineHeight).toFloat() / LINE_HEIGHT_TENTHS_DENOMINATOR).em,
                color = MaterialTheme.colorScheme.onSurface,
                style = LocalTextStyle.current.copy(lineHeightStyle = PreviewLineHeightStyle),
            )
        }
    }
}

@Composable
private fun ReaderFontFamilyTileRow(
    title: String,
    selected: ReaderFontFamily,
    onSelected: (ReaderFontFamily) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.medium),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.regular)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReaderFontFamily.entries.forEach { family ->
                ReaderFontFamilyTile(
                    label = family.displayName(strings),
                    family = family,
                    isSelected = family == selected,
                    onClick = { onSelected(family) },
                    modifier = Modifier.testTag(FONT_OPTION_TAG_PREFIX + family.name),
                )
            }
        }
        if (selected.isLatinScriptOnly) {
            Text(
                text = strings.readerFontLatinScriptOnly,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.medium),
            )
        }
    }
}

@Composable
private fun ReaderFontFamilyTile(
    label: String,
    family: ReaderFontFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    )
    val contentColor by animateColorAsState(
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
    Surface(
        modifier = modifier
            .width(92.dp)
            .height(92.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xsmall, Alignment.CenterVertically),
        ) {
            // Each typeface has its own ascent and descent: a shared baseline and a fixed slot keep every tile aligned.
            Box(
                modifier = Modifier.fillMaxWidth().height(42.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "Aa",
                    fontFamily = rememberReaderFontFamily(family),
                    fontSize = 28.sp,
                    modifier = Modifier.paddingFromBaseline(top = 34.dp),
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    minIcon: ImageVector,
    maxIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(PaddingValues(horizontal = Spacing.regular, vertical = 12.dp)),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
        ) {
            Icon(
                imageVector = minIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SliderWithoutButtons(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = maxIcon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SliderDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.regular),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

private fun readerLineHeightValueLabel(lineHeight: Int): String {
    val tenths = LINE_HEIGHT_BASE_TENTHS + lineHeight
    return "${tenths / LINE_HEIGHT_TENTHS_DENOMINATOR}.${tenths % LINE_HEIGHT_TENTHS_DENOMINATOR}"
}

// Leading outside the first and last line would push the body out of the fixed-height preview.
private val PreviewLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

// A larger title gets one line so the body stays visible in the fixed-height preview.
internal fun previewTitleMaxLines(titleFontSize: Int): Int =
    if (titleFontSize > ReaderModeDefaults.TITLE_FONT_SIZE) 1 else 2
