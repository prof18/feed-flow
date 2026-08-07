package com.prof18.feedflow.android.widget

import com.prof18.feedflow.android.settings.widget.WidgetSettingsViewModel
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `card appearance callbacks persist optimistic state and update widget once each`() = runTest(dispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())
        val widgetUpdater = FakeWidgetUpdater()
        val viewModel = WidgetSettingsViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        advanceUntilIdle()

        var expectedAppearance = WidgetCardAppearance()
        val updates = listOf<(WidgetSettingsViewModel) -> WidgetCardAppearance>(
            { currentViewModel ->
                currentViewModel.updateCardSurfaceColor(CUSTOM_COLOR_WITHOUT_ALPHA)
                expectedAppearance.copy(surfaceColor = CUSTOM_COLOR)
            },
            { currentViewModel ->
                currentViewModel.updateCardSurfaceOpacityPercent(CUSTOM_OPACITY_PERCENT)
                expectedAppearance.copy(surfaceOpacityPercent = CUSTOM_OPACITY_PERCENT)
            },
            { currentViewModel ->
                currentViewModel.updateCardCornerRadiusDp(ODD_CORNER_RADIUS_DP)
                expectedAppearance.copy(cornerRadiusDp = NORMALIZED_CORNER_RADIUS_DP)
            },
            { currentViewModel ->
                currentViewModel.updateCardItemSeparation(WidgetCardItemSeparation.DIVIDER)
                expectedAppearance.copy(itemSeparation = WidgetCardItemSeparation.DIVIDER)
            },
            { currentViewModel ->
                currentViewModel.updateCardDividerOpacityPercent(CUSTOM_DIVIDER_OPACITY_PERCENT)
                expectedAppearance.copy(dividerOpacityPercent = CUSTOM_DIVIDER_OPACITY_PERCENT)
            },
            { currentViewModel ->
                currentViewModel.updateCardImageSizing(WidgetCardImageSizing.FILL_ROW_HEIGHT)
                expectedAppearance.copy(imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT)
            },
        )

        updates.forEachIndexed { index, update ->
            expectedAppearance = update(viewModel)

            assertEquals(expectedAppearance, viewModel.settingsState.value.cardAppearance)
            assertEquals(expectedAppearance, repository.getWidgetCardAppearance())
            assertEquals(index, widgetUpdater.updateCount)

            advanceUntilIdle()

            assertEquals(index + 1, widgetUpdater.updateCount)
        }
    }

    @Test
    fun `equivalent card appearance callbacks do not update widget`() = runTest(dispatcher) {
        val initialAppearance = WidgetCardAppearance(
            surfaceColor = CUSTOM_COLOR,
            dividerOpacityPercent = MAX_PERCENT,
        )
        val repository = WidgetSettingsRepository(MapSettings()).apply {
            setWidgetCardAppearance(initialAppearance)
        }
        val widgetUpdater = FakeWidgetUpdater()
        val viewModel = WidgetSettingsViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        advanceUntilIdle()

        viewModel.updateCardSurfaceColor(CUSTOM_COLOR_WITHOUT_ALPHA)
        viewModel.updateCardSurfaceOpacityPercent(ABOVE_MAX_PERCENT)
        viewModel.updateCardCornerRadiusDp(ODD_DEFAULT_CORNER_RADIUS_DP)
        viewModel.updateCardItemSeparation(WidgetCardItemSeparation.SPACING)
        viewModel.updateCardDividerOpacityPercent(ABOVE_MAX_PERCENT)
        viewModel.updateCardImageSizing(WidgetCardImageSizing.THUMBNAIL)
        advanceUntilIdle()

        assertEquals(initialAppearance, viewModel.settingsState.value.cardAppearance)
        assertEquals(initialAppearance, repository.getWidgetCardAppearance())
        assertEquals(0, widgetUpdater.updateCount)
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var updateCount = 0
            private set

        override suspend fun update() {
            updateCount += 1
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigurationViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `card appearance callbacks persist all six settings without an updater`() = runTest(dispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())
        val viewModel = WidgetConfigurationViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
        )
        advanceUntilIdle()

        viewModel.updateCardSurfaceColor(CUSTOM_COLOR_WITHOUT_ALPHA)
        viewModel.updateCardSurfaceOpacityPercent(CUSTOM_OPACITY_PERCENT)
        viewModel.updateCardCornerRadiusDp(ODD_CORNER_RADIUS_DP)
        viewModel.updateCardItemSeparation(WidgetCardItemSeparation.DIVIDER)
        viewModel.updateCardDividerOpacityPercent(CUSTOM_DIVIDER_OPACITY_PERCENT)
        viewModel.updateCardImageSizing(WidgetCardImageSizing.FILL_ROW_HEIGHT)
        viewModel.enqueueWorker()
        advanceUntilIdle()

        val expectedAppearance = WidgetCardAppearance(
            surfaceColor = CUSTOM_COLOR,
            surfaceOpacityPercent = CUSTOM_OPACITY_PERCENT,
            cornerRadiusDp = NORMALIZED_CORNER_RADIUS_DP,
            itemSeparation = WidgetCardItemSeparation.DIVIDER,
            dividerOpacityPercent = CUSTOM_DIVIDER_OPACITY_PERCENT,
            imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
        )
        assertEquals(expectedAppearance, repository.getWidgetCardAppearance())
        assertEquals(expectedAppearance, viewModel.settingsState.value.cardAppearance)
    }

    @Test
    fun `configuration callbacks ignore normalized equivalent card appearance values`() = runTest(dispatcher) {
        val initialAppearance = WidgetCardAppearance(
            surfaceColor = CUSTOM_COLOR,
            dividerOpacityPercent = MAX_PERCENT,
        )
        val repository = WidgetSettingsRepository(MapSettings()).apply {
            setWidgetCardAppearance(initialAppearance)
        }
        val viewModel = WidgetConfigurationViewModel(
            settingsRepository = SettingsRepository(MapSettings()),
            widgetSettingsRepository = repository,
        )
        advanceUntilIdle()

        viewModel.updateCardSurfaceColor(CUSTOM_COLOR_WITHOUT_ALPHA)
        viewModel.updateCardSurfaceOpacityPercent(ABOVE_MAX_PERCENT)
        viewModel.updateCardCornerRadiusDp(ODD_DEFAULT_CORNER_RADIUS_DP)
        viewModel.updateCardItemSeparation(WidgetCardItemSeparation.SPACING)
        viewModel.updateCardDividerOpacityPercent(ABOVE_MAX_PERCENT)
        viewModel.updateCardImageSizing(WidgetCardImageSizing.THUMBNAIL)
        advanceUntilIdle()

        assertEquals(initialAppearance, repository.getWidgetCardAppearance())
        assertEquals(initialAppearance, viewModel.settingsState.value.cardAppearance)
    }
}

private const val CUSTOM_COLOR_WITHOUT_ALPHA = 0x00123456
private const val CUSTOM_COLOR = 0xFF123456.toInt()
private const val CUSTOM_OPACITY_PERCENT = 72
private const val CUSTOM_DIVIDER_OPACITY_PERCENT = 48
private const val ODD_CORNER_RADIUS_DP = 17
private const val NORMALIZED_CORNER_RADIUS_DP = 18
private const val ODD_DEFAULT_CORNER_RADIUS_DP = 15
private const val MAX_PERCENT = 100
private const val ABOVE_MAX_PERCENT = 101
