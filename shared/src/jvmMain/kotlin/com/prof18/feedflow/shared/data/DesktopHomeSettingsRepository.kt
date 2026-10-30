package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Base64

class DesktopHomeSettingsRepository(
    private val settings: Settings,
) {
    private val isMultiPaneLayoutEnabledMutableFlow = MutableStateFlow(isMultiPaneLayoutEnabled())
    val isMultiPaneLayoutEnabledFlow: StateFlow<Boolean> = isMultiPaneLayoutEnabledMutableFlow.asStateFlow()

    fun isMultiPaneLayoutEnabled(): Boolean =
        settings.getBoolean(
            DesktopHomeSettingsFields.DESKTOP_HOME_MULTI_PANE_LAYOUT_ENABLED.name,
            defaultValue = true,
        )

    fun setMultiPaneLayoutEnabled(value: Boolean) {
        settings.set(DesktopHomeSettingsFields.DESKTOP_HOME_MULTI_PANE_LAYOUT_ENABLED.name, value)
        isMultiPaneLayoutEnabledMutableFlow.update { value }
    }

    fun isDrawerVisible(): Boolean =
        settings.getBoolean(
            DesktopHomeSettingsFields.DESKTOP_HOME_DRAWER_VISIBLE.name,
            defaultValue = true,
        )

    fun setDrawerVisible(value: Boolean) =
        settings.set(DesktopHomeSettingsFields.DESKTOP_HOME_DRAWER_VISIBLE.name, value)

    fun getPaneExpansionIndex(): Int =
        settings.getInt(
            DesktopHomeSettingsFields.DESKTOP_HOME_PANE_EXPANSION_INDEX.name,
            defaultValue = DEFAULT_PANE_EXPANSION_INDEX,
        )

    fun setPaneExpansionIndex(value: Int) =
        settings.set(DesktopHomeSettingsFields.DESKTOP_HOME_PANE_EXPANSION_INDEX.name, value)

    fun getRequestedSidebarWidth(): Float? =
        settings.getFloatOrNull(DesktopHomeSettingsFields.DESKTOP_HOME_REQUESTED_SIDEBAR_WIDTH.name)
            ?.takeIf { it.isFinite() && it > 0f }

    fun setRequestedSidebarWidth(value: Float) =
        settings.set(DesktopHomeSettingsFields.DESKTOP_HOME_REQUESTED_SIDEBAR_WIDTH.name, value)

    fun getRequestedListWidth(): Float? =
        settings.getFloatOrNull(DesktopHomeSettingsFields.DESKTOP_HOME_REQUESTED_LIST_WIDTH.name)
            ?.takeIf { it.isFinite() && it > 0f }

    fun setRequestedListWidth(value: Float) =
        settings.set(DesktopHomeSettingsFields.DESKTOP_HOME_REQUESTED_LIST_WIDTH.name, value)

    fun getCollapsedFeedTreeSections(): Set<String> {
        val encoded = settings.getStringOrNull(DesktopHomeSettingsFields.DESKTOP_HOME_COLLAPSED_FEED_TREE_SECTIONS.name)
            ?: return emptySet()
        if (encoded.isEmpty()) return emptySet()
        return runCatching {
            encoded.split(',').map { Base64.getUrlDecoder().decode(it).toString(Charsets.UTF_8) }
                .filter { it == "uncategorized" || it.startsWith("category:") }
                .toSet()
        }.getOrDefault(emptySet())
    }

    fun setCollapsedFeedTreeSections(value: Set<String>) =
        settings.set(
            DesktopHomeSettingsFields.DESKTOP_HOME_COLLAPSED_FEED_TREE_SECTIONS.name,
            value.sorted().joinToString(",") { Base64.getUrlEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) },
        )

    private companion object {
        const val DEFAULT_PANE_EXPANSION_INDEX = 5
    }
}

private enum class DesktopHomeSettingsFields {
    DESKTOP_HOME_MULTI_PANE_LAYOUT_ENABLED,
    DESKTOP_HOME_DRAWER_VISIBLE,
    DESKTOP_HOME_PANE_EXPANSION_INDEX,
    DESKTOP_HOME_REQUESTED_SIDEBAR_WIDTH,
    DESKTOP_HOME_REQUESTED_LIST_WIDTH,
    DESKTOP_HOME_COLLAPSED_FEED_TREE_SECTIONS,
}
