package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.domain.readerLineHeightLabel
import com.prof18.feedflow.shared.domain.readerLineHeightToTextLineHeightSp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Slider
import org.jetbrains.jewel.ui.component.Text

private const val PREVIEW_FADE_START = 0.75f
private const val PREVIEW_TITLE_GAP_RATIO = 0.35f
private const val TITLE_LINE_HEIGHT_DELTA = 8
private val PreviewShape = RoundedCornerShape(12.dp)
private val FontChipShape = RoundedCornerShape(8.dp)

@Composable
internal fun ReaderTextSettingsControls(
    fontSize: Int,
    lineHeight: Int,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    headlineFont: ReaderFontFamily = ReaderModeDefaults.HEADLINE_FONT,
    bodyFont: ReaderFontFamily = ReaderModeDefaults.BODY_FONT,
    onHeadlineFontChange: (ReaderFontFamily) -> Unit = {},
    onBodyFontChange: (ReaderFontFamily) -> Unit = {},
    titleFontSize: Int = ReaderModeDefaults.TITLE_FONT_SIZE,
    onTitleFontSizeChange: (Int) -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 12.dp

    Column(
        modifier = modifier
            .widthIn(min = 280.dp, max = 400.dp)
            .heightIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.orEmpty(),
                style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                modifier = Modifier.heightIn(min = actionHeight),
                onClick = {
                    onFontSizeChange(ReaderModeDefaults.FONT_SIZE)
                    onLineHeightChange(ReaderModeDefaults.LINE_HEIGHT)
                    onTitleFontSizeChange(ReaderModeDefaults.TITLE_FONT_SIZE)
                    onHeadlineFontChange(ReaderModeDefaults.HEADLINE_FONT)
                    onBodyFontChange(ReaderModeDefaults.BODY_FONT)
                },
                enabled = fontSize != ReaderModeDefaults.FONT_SIZE ||
                    lineHeight != ReaderModeDefaults.LINE_HEIGHT ||
                    titleFontSize != ReaderModeDefaults.TITLE_FONT_SIZE ||
                    headlineFont != ReaderModeDefaults.HEADLINE_FONT ||
                    bodyFont != ReaderModeDefaults.BODY_FONT,
            ) { Text(strings.readerModeResetToDefault) }
        }

        ReaderTextSettingsPreview(
            fontSize = fontSize,
            lineHeight = lineHeight,
            titleFontSize = titleFontSize,
            headlineFont = headlineFont,
            bodyFont = bodyFont,
        )

        ReaderFontChoice(
            title = strings.readerModeFont,
            selected = bodyFont,
            onSelected = { family ->
                onHeadlineFontChange(family)
                onBodyFontChange(family)
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderTextSettingLabel(strings.readerModeFontSize, fontSize.toString())
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 12f..40f,
                steps = 27,
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = strings.readerModeFontSize },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderTextSettingLabel(strings.readerModeTitleFontSize, titleFontSize.toString())
            Slider(
                value = titleFontSize.toFloat(),
                onValueChange = { onTitleFontSizeChange(it.toInt()) },
                valueRange = 16f..48f,
                steps = 31,
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = strings.readerModeTitleFontSize },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderTextSettingLabel(strings.readerModeLineHeight, readerLineHeightLabel(lineHeight))
            Slider(
                value = lineHeight.toFloat(),
                onValueChange = { onLineHeightChange(it.toInt()) },
                valueRange = 0f..15f,
                steps = 14,
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = strings.readerModeLineHeight },
            )
        }
    }
}

// Fixed height, so dragging a size slider never moves the slider under the pointer.
@Composable
private fun ReaderTextSettingsPreview(
    fontSize: Int,
    lineHeight: Int,
    titleFontSize: Int,
    headlineFont: ReaderFontFamily,
    bodyFont: ReaderFontFamily,
) {
    val strings = LocalFeedFlowStrings.current
    val textStyle = DesktopDesignTokens.chromeText.copy(color = JewelTheme.globalColors.text.normal)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(PreviewShape)
            .background(JewelTheme.globalColors.panelBackground)
            .border(1.dp, JewelTheme.globalColors.borders.normal, PreviewShape)
            .clearAndSetSemantics {}
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(PREVIEW_FADE_START to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy((titleFontSize * PREVIEW_TITLE_GAP_RATIO).dp),
    ) {
        Text(
            text = strings.readerModePreviewTitle,
            style = textStyle.copy(
                fontSize = titleFontSize.sp,
                lineHeight = (titleFontSize + TITLE_LINE_HEIGHT_DELTA).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = rememberReaderComposeFontFamily(headlineFont) ?: textStyle.fontFamily,
                lineHeightStyle = PreviewLineHeightStyle,
            ),
            maxLines = previewTitleMaxLines(titleFontSize),
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = strings.readerModePreviewBody,
            style = textStyle.copy(
                fontSize = fontSize.sp,
                lineHeight = readerLineHeightToTextLineHeightSp(fontSize, lineHeight).sp,
                fontFamily = rememberReaderComposeFontFamily(bodyFont) ?: textStyle.fontFamily,
                lineHeightStyle = PreviewLineHeightStyle,
            ),
        )
    }
}

@Composable
private fun ReaderFontChoice(
    title: String,
    selected: ReaderFontFamily,
    onSelected: (ReaderFontFamily) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = JewelTheme.globalColors.text.info)
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderFontFamily.entries.forEach { family ->
                ReaderFontChip(
                    label = family.displayName(strings),
                    family = family,
                    isSelected = family == selected,
                    onClick = { onSelected(family) },
                )
            }
        }
    }
}

@Composable
private fun ReaderFontChip(
    label: String,
    family: ReaderFontFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accent = JewelTheme.globalColors.outlines.focused
    val textStyle = DesktopDesignTokens.chromeText.copy(
        color = if (isSelected) accent else JewelTheme.globalColors.text.normal,
        fontFamily = rememberReaderComposeFontFamily(family) ?: DesktopDesignTokens.chromeText.fontFamily,
    )
    Text(
        text = label,
        style = textStyle.copy(fontSize = 15.sp),
        modifier = Modifier
            .clip(FontChipShape)
            .background(if (isSelected) accent.copy(alpha = 0.12f) else JewelTheme.globalColors.panelBackground)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accent else JewelTheme.globalColors.borders.normal,
                shape = FontChipShape,
            )
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ReaderTextSettingLabel(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = JewelTheme.globalColors.text.info)
    }
}

private fun ReaderFontFamily.displayName(strings: FeedFlowStrings): String = when (this) {
    ReaderFontFamily.SYSTEM -> strings.readerFontSystem
    ReaderFontFamily.LITERATA -> strings.readerFontLiterata
    ReaderFontFamily.SOURCE_SERIF_4 -> strings.readerFontSourceSerif4
    ReaderFontFamily.LORA -> strings.readerFontLora
    ReaderFontFamily.ATKINSON_HYPERLEGIBLE_NEXT -> strings.readerFontAtkinsonHyperlegibleNext
}

// Leading outside the first and last line would push the body out of the fixed-height preview.
private val PreviewLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

// A larger title gets one line so the body stays visible in the fixed-height preview.
internal fun previewTitleMaxLines(titleFontSize: Int): Int =
    if (titleFontSize > ReaderModeDefaults.TITLE_FONT_SIZE) 1 else 2
