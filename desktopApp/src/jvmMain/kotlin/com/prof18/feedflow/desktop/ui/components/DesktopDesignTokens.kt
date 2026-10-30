package com.prof18.feedflow.desktop.ui.components

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object DesktopDesignTokens {
    val fontFamily = FontFamily(plexFont(FontWeight.Normal), plexFont(FontWeight.Medium), plexFont(FontWeight.SemiBold))
    val chromeText = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, lineHeight = 20.sp)
    val metadataText = chromeText.copy(fontSize = 12.sp, lineHeight = 18.sp)
    val sectionHeading = chromeText.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)

    val spaceSmall = 4.dp
    val spaceMedium = 8.dp
    val spaceLarge = 12.dp
    val spaceSection = 16.dp
    val spacePanel = 24.dp
    val spaceReading = 32.dp

    val navigationRowMinHeight = 32.dp
    val settingsRowMinHeight = 36.dp
    val iconActionMinSize = 32.dp
    val iconActionLargeSize = 36.dp
    val toolbarMinHeight = 56.dp
    val buttonCornerSize = 8.dp
    val selectionCornerSize = 12.dp

    private fun plexFont(weight: FontWeight) = Font(
        resource = "fonts/IBMPlexSans.ttf",
        weight = weight,
        variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
    )
}
