package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.PreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SettingDropdownOption<T>(
    val value: T,
    val label: String,
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
    val currentLabel = options.firstOrNull { it.value == currentValue }?.label.orEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
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
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedMenuShape,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp,
            ) {
                options.forEach { option ->
                    val isSelected = option.value == currentValue
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            )
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

private val RoundedDropdownShape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
private val RoundedMenuShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)

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
