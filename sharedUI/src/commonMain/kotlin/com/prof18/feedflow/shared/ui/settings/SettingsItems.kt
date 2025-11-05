package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun HideDescriptionSwitch(
    isHideDescriptionEnabled: Boolean,
    modifier: Modifier = Modifier,
    setHideDescription: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                setHideDescription(!isHideDescriptionEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.SubtitlesOff,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsHideDescription,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isHideDescriptionEnabled,
            onCheckedChange = setHideDescription,
        )
    }
}

@Composable
fun HideImagesSwitch(
    isHideImagesEnabled: Boolean,
    modifier: Modifier = Modifier,
    setHideImages: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                setHideImages(!isHideImagesEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.HideImage,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsHideImages,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isHideImagesEnabled,
            onCheckedChange = setHideImages,
        )
    }
}

@Composable
fun HideDateSwitch(
    isHideDateEnabled: Boolean,
    modifier: Modifier = Modifier,
    setHideDate: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                setHideDate(!isHideDateEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.HideImage,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsHideDate,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isHideDateEnabled,
            onCheckedChange = setHideDate,
        )
    }
}

@Composable
fun RemoveTitleFromDescSwitch(
    isRemoveTitleFromDescriptionEnabled: Boolean,
    modifier: Modifier = Modifier,
    setRemoveTitleFromDescription: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                setRemoveTitleFromDescription(!isRemoveTitleFromDescriptionEnabled)
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.HideSource,
            contentDescription = null,
        )

        Text(
            text = LocalFeedFlowStrings.current.settingsHideDuplicatedTitleFromDesc,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
        )
        Switch(
            interactionSource = interactionSource,
            checked = isRemoveTitleFromDescriptionEnabled,
            onCheckedChange = setRemoveTitleFromDescription,
        )
    }
}
