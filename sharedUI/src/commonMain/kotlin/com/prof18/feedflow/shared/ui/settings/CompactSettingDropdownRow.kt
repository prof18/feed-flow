package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.PreviewTheme
import com.prof18.feedflow.shared.ui.utils.exposeTestTagsAsResourceIds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * @param shortLabel shown in the collapsed row instead of [label], when the full label is too
 * verbose to fit the pill.
 * @param sectionHeader when set, a non-clickable header rendered above this option, grouping it
 * with the options that follow.
 */
data class SettingDropdownOption<T>(
    val value: T,
    val label: String,
    val e2eId: String? = null,
    val subtitle: String? = null,
    val shortLabel: String? = null,
    val sectionHeader: String? = null,
)

@Composable
fun <T> CompactSettingDropdownRow(
    title: String,
    currentValue: T,
    options: ImmutableList<SettingDropdownOption<T>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.regular,
        vertical = Spacing.small,
    ),
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOption = options.firstOrNull { it.value == currentValue }
    val currentLabel = currentOption?.let { it.shortLabel ?: it.label }.orEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        Box {
            Surface(
                shape = RoundedDropdownShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(RoundedDropdownShape)
                    .clickable { expanded = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.regular, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xsmall),
                ) {
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                    )
                }
            }

            DropdownMenu(
                modifier = Modifier.exposeTestTagsAsResourceIds(),
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp,
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = option.value == currentValue
                    if (option.sectionHeader != null) {
                        DropdownSectionHeader(
                            title = option.sectionHeader,
                            isFirst = index == 0,
                        )
                    }
                    DropdownMenuItem(
                        modifier = option.e2eId?.let { Modifier.testTag(it) } ?: Modifier,
                        text = {
                            Column {
                                Text(
                                    text = option.label,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                )
                                if (option.subtitle != null) {
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        onClick = {
                            onOptionSelected(option.value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownSectionHeader(
    title: String,
    isFirst: Boolean,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = Spacing.regular,
            end = Spacing.regular,
            top = if (isFirst) Spacing.small else Spacing.regular,
            bottom = Spacing.xsmall,
        ),
    )
}

private val RoundedDropdownShape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)

@Preview
@Composable
private fun CompactSettingDropdownRowPreview() {
    PreviewTheme {
        CompactSettingDropdownRow(
            title = "Date format",
            currentValue = "Day/Month",
            options = persistentListOf(
                SettingDropdownOption("Day/Month", "Day/Month"),
                SettingDropdownOption("Month/Day", "Month/Day"),
                SettingDropdownOption("ISO", "ISO"),
            ),
            onOptionSelected = {},
        )
    }
}
