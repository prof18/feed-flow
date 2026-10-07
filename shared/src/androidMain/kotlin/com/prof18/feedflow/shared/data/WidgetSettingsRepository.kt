package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class WidgetSettingsRepository(
    private val settings: Settings,
) {
    private val configurationRevision = MutableStateFlow(0L)

    fun observeWidgetConfiguration(widgetId: Int): Flow<WidgetConfiguration> =
        configurationRevision
            .map { getWidgetConfiguration(widgetId) }
            .distinctUntilChanged()

    fun getWidgetConfiguration(widgetId: Int): WidgetConfiguration {
        val storedBackgroundColor = settings.getStringOrNull(
            perWidgetKey(WidgetSettingsFields.WIDGET_BACKGROUND_COLOR, widgetId),
        )

        return WidgetConfiguration(
            contentFilter = WidgetContentFilter.deserialize(
                settings.getStringOrNull(
                    perWidgetKey(WidgetSettingsFields.WIDGET_CONTENT_FILTER, widgetId),
                ),
            ),
            feedLayout = settings.getStringOrNull(
                perWidgetKey(WidgetSettingsFields.FEED_WIDGET_LAYOUT, widgetId),
            )?.toEnumOrNull<WidgetFeedLayout>() ?: getFeedWidgetLayout(),
            showHeader = settings.getBooleanOrNull(
                perWidgetKey(WidgetSettingsFields.WIDGET_SHOW_HEADER, widgetId),
            ) ?: getWidgetShowHeader(),
            showRefreshButton = settings.getBooleanOrNull(
                perWidgetKey(WidgetSettingsFields.WIDGET_SHOW_REFRESH_BUTTON, widgetId),
            ) ?: getWidgetShowRefreshButton(),
            fontScale = settings.getIntOrNull(
                perWidgetKey(WidgetSettingsFields.WIDGET_FONT_SCALE_FACTOR, widgetId),
            ) ?: getWidgetFontScaleFactor(),
            backgroundColor = when (storedBackgroundColor) {
                DEFAULT_WIDGET_BACKGROUND_COLOR -> null
                null -> getWidgetBackgroundColor()
                else -> storedBackgroundColor.toIntOrNull()
            },
            backgroundOpacityPercent = settings.getIntOrNull(
                perWidgetKey(WidgetSettingsFields.WIDGET_BACKGROUND_OPACITY_PERCENT, widgetId),
            ) ?: getWidgetBackgroundOpacityPercent(),
            textColorMode = settings.getStringOrNull(
                perWidgetKey(WidgetSettingsFields.WIDGET_TEXT_COLOR_MODE, widgetId),
            )?.toEnumOrNull<WidgetTextColorMode>() ?: getWidgetTextColorMode(),
            hideImages = settings.getBooleanOrNull(
                perWidgetKey(WidgetSettingsFields.WIDGET_HIDE_IMAGES, widgetId),
            ) ?: getWidgetHideImages(),
        )
    }

    fun setWidgetContentFilter(widgetId: Int, filter: WidgetContentFilter) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_CONTENT_FILTER, widgetId)] =
            filter.serialize()
        configurationRevision.update { it + 1 }
    }

    fun setWidgetFeedLayout(widgetId: Int, feedLayout: WidgetFeedLayout) {
        settings[perWidgetKey(WidgetSettingsFields.FEED_WIDGET_LAYOUT, widgetId)] = feedLayout.name
        configurationRevision.update { it + 1 }
    }

    fun setWidgetShowHeader(widgetId: Int, showHeader: Boolean) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_SHOW_HEADER, widgetId)] = showHeader
        configurationRevision.update { it + 1 }
    }

    fun setWidgetShowRefreshButton(widgetId: Int, showRefreshButton: Boolean) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_SHOW_REFRESH_BUTTON, widgetId)] = showRefreshButton
        configurationRevision.update { it + 1 }
    }

    fun setWidgetFontScaleFactor(widgetId: Int, scaleFactor: Int) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_FONT_SCALE_FACTOR, widgetId)] = scaleFactor
        configurationRevision.update { it + 1 }
    }

    fun setWidgetBackgroundColor(widgetId: Int, colorArgb: Int?) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_BACKGROUND_COLOR, widgetId)] =
            colorArgb?.toString() ?: DEFAULT_WIDGET_BACKGROUND_COLOR
        configurationRevision.update { it + 1 }
    }

    fun setWidgetBackgroundOpacityPercent(widgetId: Int, opacityPercent: Int) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_BACKGROUND_OPACITY_PERCENT, widgetId)] =
            opacityPercent
        configurationRevision.update { it + 1 }
    }

    fun setWidgetTextColorMode(widgetId: Int, textColorMode: WidgetTextColorMode) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_TEXT_COLOR_MODE, widgetId)] =
            textColorMode.name
        configurationRevision.update { it + 1 }
    }

    fun setWidgetHideImages(widgetId: Int, hideImages: Boolean) {
        settings[perWidgetKey(WidgetSettingsFields.WIDGET_HIDE_IMAGES, widgetId)] = hideImages
        configurationRevision.update { it + 1 }
    }

    fun clearWidgetConfiguration(widgetId: Int) {
        WidgetSettingsFields.entries.forEach { field ->
            settings.remove(perWidgetKey(field, widgetId))
        }
        configurationRevision.update { it + 1 }
    }

    fun restoreWidgetConfigurations(oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        if (oldWidgetIds.isEmpty()) return

        val restoredConfigurations = oldWidgetIds
            .zip(newWidgetIds)
            .map { (oldWidgetId, newWidgetId) ->
                newWidgetId to getWidgetConfiguration(oldWidgetId)
            }

        oldWidgetIds.forEach { oldWidgetId ->
            WidgetSettingsFields.entries.forEach { field ->
                settings.remove(perWidgetKey(field, oldWidgetId))
            }
        }

        restoredConfigurations.forEach { (newWidgetId, configuration) ->
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_CONTENT_FILTER, newWidgetId)] =
                configuration.contentFilter.serialize()
            settings[perWidgetKey(WidgetSettingsFields.FEED_WIDGET_LAYOUT, newWidgetId)] =
                configuration.feedLayout.name
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_SHOW_HEADER, newWidgetId)] =
                configuration.showHeader
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_SHOW_REFRESH_BUTTON, newWidgetId)] =
                configuration.showRefreshButton
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_FONT_SCALE_FACTOR, newWidgetId)] =
                configuration.fontScale
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_BACKGROUND_COLOR, newWidgetId)] =
                configuration.backgroundColor?.toString() ?: DEFAULT_WIDGET_BACKGROUND_COLOR
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_BACKGROUND_OPACITY_PERCENT, newWidgetId)] =
                configuration.backgroundOpacityPercent
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_TEXT_COLOR_MODE, newWidgetId)] =
                configuration.textColorMode.name
            settings[perWidgetKey(WidgetSettingsFields.WIDGET_HIDE_IMAGES, newWidgetId)] =
                configuration.hideImages
        }

        configurationRevision.update { it + 1 }
    }

    private fun getFeedWidgetLayout(): WidgetFeedLayout =
        settings.getString(
            WidgetSettingsFields.FEED_WIDGET_LAYOUT.name,
            WidgetFeedLayout.LIST.name,
        ).toEnumOrNull<WidgetFeedLayout>() ?: WidgetFeedLayout.LIST

    private fun getWidgetShowHeader(): Boolean =
        settings.getBoolean(WidgetSettingsFields.WIDGET_SHOW_HEADER.name, true)

    private fun getWidgetShowRefreshButton(): Boolean =
        settings.getBoolean(WidgetSettingsFields.WIDGET_SHOW_REFRESH_BUTTON.name, false)

    private fun getWidgetFontScaleFactor(): Int =
        settings.getInt(
            WidgetSettingsFields.WIDGET_FONT_SCALE_FACTOR.name,
            DEFAULT_WIDGET_FONT_SCALE_FACTOR,
        )

    private fun getWidgetBackgroundColor(): Int? =
        settings.getIntOrNull(WidgetSettingsFields.WIDGET_BACKGROUND_COLOR.name)

    private fun getWidgetBackgroundOpacityPercent(): Int =
        settings.getInt(
            WidgetSettingsFields.WIDGET_BACKGROUND_OPACITY_PERCENT.name,
            DEFAULT_WIDGET_BACKGROUND_OPACITY_PERCENT,
        )

    private fun getWidgetTextColorMode(): WidgetTextColorMode =
        settings.getString(
            WidgetSettingsFields.WIDGET_TEXT_COLOR_MODE.name,
            WidgetTextColorMode.AUTOMATIC.name,
        ).toEnumOrNull<WidgetTextColorMode>() ?: WidgetTextColorMode.AUTOMATIC

    private fun getWidgetHideImages(): Boolean =
        settings.getBoolean(WidgetSettingsFields.WIDGET_HIDE_IMAGES.name, false)

    private companion object {
        const val DEFAULT_WIDGET_FONT_SCALE_FACTOR = 0
        const val DEFAULT_WIDGET_BACKGROUND_OPACITY_PERCENT = 100
        const val DEFAULT_WIDGET_BACKGROUND_COLOR = "default"
    }
}

data class WidgetConfiguration(
    val contentFilter: WidgetContentFilter,
    val feedLayout: WidgetFeedLayout,
    val showHeader: Boolean,
    val showRefreshButton: Boolean = false,
    val fontScale: Int,
    val backgroundColor: Int?,
    val backgroundOpacityPercent: Int,
    val textColorMode: WidgetTextColorMode,
    val hideImages: Boolean,
)

private enum class WidgetSettingsFields {
    FEED_WIDGET_LAYOUT,
    WIDGET_SHOW_HEADER,
    WIDGET_SHOW_REFRESH_BUTTON,
    WIDGET_FONT_SCALE_FACTOR,
    WIDGET_BACKGROUND_COLOR,
    WIDGET_BACKGROUND_OPACITY_PERCENT,
    WIDGET_TEXT_COLOR_MODE,
    WIDGET_HIDE_IMAGES,
    WIDGET_CONTENT_FILTER,
}

private fun perWidgetKey(field: WidgetSettingsFields, widgetId: Int): String =
    field.name + "_" + widgetId

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    enumValues<T>().firstOrNull { it.name == this }
