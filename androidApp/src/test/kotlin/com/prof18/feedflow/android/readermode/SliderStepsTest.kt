package com.prof18.feedflow.android.readermode

import com.prof18.feedflow.android.ui.readermode.sliderStepSize
import com.prof18.feedflow.android.ui.readermode.wholeValueSteps
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals

class SliderStepsTest {
    @Test
    fun `whole value steps give one slider position per integer in the range`() {
        listOf(-4f..16f, -4f..12f, 12f..40f, 0f..15f).forEach { range ->
            val positions = tickValues(range, wholeValueSteps(range))
            val expected = (range.start.roundToInt()..range.endInclusive.roundToInt()).toList()

            assertEquals(expected, positions.map { it.roundToInt() }, "range $range")
            positions.forEach { assertEquals(it.roundToInt().toFloat(), it, absoluteTolerance = 1e-4f) }
        }
    }

    @Test
    fun `whole value steps match the reader slider step counts`() {
        assertEquals(27, wholeValueSteps(12f..40f))
        assertEquals(14, wholeValueSteps(0f..15f))
        assertEquals(19, wholeValueSteps(-4f..16f))
        assertEquals(15, wholeValueSteps(-4f..12f))
    }

    @Test
    fun `plus and minus buttons move by one slider position`() {
        assertEquals(1f, sliderStepSize(-4f..16f, wholeValueSteps(-4f..16f)))
        assertEquals(1f, sliderStepSize(-4f..12f, wholeValueSteps(-4f..12f)))
        assertEquals(0.5f, sliderStepSize(0f..2f, steps = 3))
        assertEquals(0f, sliderStepSize(0f..1f, steps = 0))
    }

    // Same tick layout as compose-unstyled's stepsToTickFractions.
    private fun tickValues(range: ClosedFloatingPointRange<Float>, steps: Int): List<Float> =
        (0..steps + 1).map { index ->
            range.start + (range.endInclusive - range.start) * index / (steps + 1)
        }
}
