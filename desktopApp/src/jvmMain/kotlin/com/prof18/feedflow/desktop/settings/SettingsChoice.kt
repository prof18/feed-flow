package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.settings.search.settingsSearchHighlight
import com.prof18.feedflow.desktop.ui.components.DesktopChoiceDropdown
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun <T : Any> SettingsChoice(
    title: String,
    choices: ImmutableList<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    val fontScale = LocalDensity.current.fontScale
    val inSection = LocalInSettingsSection.current
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .settingsSearchHighlight(title)
            .settingsRowSeparator()
            .padding(horizontal = if (inSection) 14.dp else 4.dp, vertical = 8.dp),
    ) {
        val stacked = maxWidth < 440.dp || fontScale > INLINE_MAX_FONT_SCALE
        val controlWidth = if (stacked) maxWidth else 220.dp
        val label: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title)
                if (supportingText != null) {
                    Text(
                        supportingText,
                        style = DesktopDesignTokens.metadataText,
                        color = JewelTheme.globalColors.text.info,
                    )
                }
            }
        }
        val control: @Composable () -> Unit = {
            DesktopChoiceDropdown(
                title = title,
                choices = choices,
                selected = selected,
                onSelected = onSelected,
                enabled = enabled,
                maxPopupWidth = maxWidth.coerceAtMost(440.dp),
                modifier = Modifier.width(controlWidth),
            )
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                label()
                control()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { label() }
                control()
            }
        }
    }
}

private const val INLINE_MAX_FONT_SCALE = 1.4f
