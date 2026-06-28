package com.prof18.feedflow.shared.ui.readermode

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledSlider
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun SliderWithPlusMinus(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    increaseContentDescription: String =
        LocalFeedFlowStrings.current.increaseFontSizeButtonContentDescription,
    decreaseContentDescription: String =
        LocalFeedFlowStrings.current.decreaseFontSizeButtonContentDescription,
) {
    val stepSize = if (steps == 0) {
        0f
    } else {
        (valueRange.endInclusive - valueRange.start) / steps
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubtractButton(
            onClick = { onValueChange((value - stepSize).coerceAtLeast(valueRange.start)) },
            modifier = Modifier.weight(1f),
            enabled = enabled && value > valueRange.start,
            contentDescription = decreaseContentDescription,
        )
        UnstyledSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(weight = 10f),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            interactionSource = interactionSource,
            track = { state ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(state.fraction)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            },
            thumb = {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            },
        )
        AddButton(
            onClick = { onValueChange((value + stepSize).coerceAtMost(valueRange.endInclusive)) },
            modifier = Modifier.weight(1f),
            enabled = enabled && value < valueRange.endInclusive,
            contentDescription = increaseContentDescription,
        )
    }
}

@Composable
fun SliderWithoutButtons(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    UnstyledSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        track = { state ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(state.fraction)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        },
        thumb = {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        },
    )
}

@Composable
fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String =
        LocalFeedFlowStrings.current.increaseFontSizeButtonContentDescription,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SubtractButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String =
        LocalFeedFlowStrings.current.decreaseFontSizeButtonContentDescription,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) {
        Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
