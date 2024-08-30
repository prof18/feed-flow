package com.prof18.feedflow.shared.ui.readermode

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Adapted from https://github.com/omnivore-app/omnivore/blob/main/android/Omnivore/app/src/main/java/app/omnivore/omnivore/feature/components/SliderWithPlusMinus.kt#L91
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
) {
    var sliderValue by remember { mutableFloatStateOf(value) }
    val sliderValueStepSize = if (steps == 0) {
        0f
    } else {
        (valueRange.endInclusive - valueRange.start) / steps
    }

    Row(
        modifier = modifier,
    ) {
        SubtractButton(
            onClick = {
                sliderValue -= sliderValueStepSize
                onValueChange(sliderValue)
            },
            modifier = Modifier.weight(1f),
            enabled = sliderValue > valueRange.start,
        )
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            modifier = modifier.weight(10f),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors().copy(
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            interactionSource = interactionSource,
        )
        AddButton(
            onClick = {
                sliderValue += sliderValueStepSize
                onValueChange(sliderValue)
            },
            modifier = Modifier
                .weight(1f),
            enabled = sliderValue < valueRange.endInclusive,
        )
    }
}

@Composable
fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    IconButton(
        onClick,
        modifier,
        enabled,
        colors,
        interactionSource,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = if (enabled) colors.disabledContentColor else colors.disabledContentColor,
        )
    }
}

@Composable
fun SubtractButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    IconButton(
        onClick,
        modifier,
        enabled,
        colors,
        interactionSource,
    ) {
        Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = null,
            tint = if (enabled) colors.disabledContentColor else colors.disabledContentColor,
        )
    }
}
