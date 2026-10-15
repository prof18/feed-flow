package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.domain.readerLineHeightLabel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Slider
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun ReaderTextSettingsControls(
    fontSize: Int,
    lineHeight: Int,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp

    Column(
        modifier = modifier
            .widthIn(min = 280.dp, max = 360.dp)
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
        OutlinedButton(
            modifier = Modifier.heightIn(min = actionHeight),
            onClick = {
                onFontSizeChange(ReaderModeDefaults.FONT_SIZE)
                onLineHeightChange(ReaderModeDefaults.LINE_HEIGHT)
            },
            enabled = fontSize != ReaderModeDefaults.FONT_SIZE || lineHeight != ReaderModeDefaults.LINE_HEIGHT,
        ) { Text(strings.readerModeResetToDefault) }
    }
}

@Composable
private fun ReaderTextSettingLabel(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value)
    }
}
