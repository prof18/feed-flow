package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FeedAppearanceSettingsRepository(
    private val settings: Settings,
) {
    private val swipeActionsMutableFlow = MutableStateFlow(
        SwipeActions(
            leftSwipeAction = getSwipeAction(SwipeDirection.LEFT),
            rightSwipeAction = getSwipeAction(SwipeDirection.RIGHT),
        ),
    )
    val swipeActions: StateFlow<SwipeActions> = swipeActionsMutableFlow.asStateFlow()

    private val feedLayoutMutableFlow = MutableStateFlow(getFeedLayout())
    val feedLayout: StateFlow<FeedLayout> = feedLayoutMutableFlow.asStateFlow()

    fun getRemoveTitleFromDescription(): Boolean =
        settings.getBoolean(FeedAppearanceSettingsFields.REMOVE_TITLE_FROM_DESCRIPTION.name, false)

    fun setRemoveTitleFromDescription(value: Boolean) =
        settings.set(FeedAppearanceSettingsFields.REMOVE_TITLE_FROM_DESCRIPTION.name, value)

    fun getHideDescription(): Boolean =
        settings.getBoolean(FeedAppearanceSettingsFields.HIDE_DESCRIPTION.name, false)

    fun setHideDescription(value: Boolean) =
        settings.set(FeedAppearanceSettingsFields.HIDE_DESCRIPTION.name, value)

    fun getFeedListFontScaleFactor(): Int =
        settings.getInt(
            FeedAppearanceSettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name,
            DEFAULT_FEED_LIST_FONT_SCALE_FACTOR,
        )

    fun setFeedListFontScaleFactor(value: Int) =
        settings.set(FeedAppearanceSettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name, value)

    fun getHideImages(): Boolean =
        settings.getBoolean(FeedAppearanceSettingsFields.HIDE_IMAGES.name, false)

    fun setHideImages(value: Boolean) =
        settings.set(FeedAppearanceSettingsFields.HIDE_IMAGES.name, value)

    fun getHideDate(): Boolean =
        settings.getBoolean(FeedAppearanceSettingsFields.HIDE_DATE.name, false)

    fun setHideDate(value: Boolean) =
        settings.set(FeedAppearanceSettingsFields.HIDE_DATE.name, value)

    fun getSwipeAction(direction: SwipeDirection): SwipeActionType {
        val fieldName = when (direction) {
            SwipeDirection.LEFT -> FeedAppearanceSettingsFields.LEFT_SWIPE_ACTION.name
            SwipeDirection.RIGHT -> FeedAppearanceSettingsFields.RIGHT_SWIPE_ACTION.name
        }
        return settings.getString(fieldName, SwipeActionType.NONE.name)
            .let { SwipeActionType.valueOf(it) }
    }

    fun setSwipeAction(direction: SwipeDirection, action: SwipeActionType) {
        val fieldName = when (direction) {
            SwipeDirection.LEFT -> FeedAppearanceSettingsFields.LEFT_SWIPE_ACTION.name
            SwipeDirection.RIGHT -> FeedAppearanceSettingsFields.RIGHT_SWIPE_ACTION.name
        }
        settings[fieldName] = action.name

        swipeActionsMutableFlow.update { currentSwipeActions ->
            when (direction) {
                SwipeDirection.LEFT -> currentSwipeActions.copy(leftSwipeAction = action)
                SwipeDirection.RIGHT -> currentSwipeActions.copy(rightSwipeAction = action)
            }
        }
    }

    fun getDateFormat(): DateFormat =
        settings.getString(FeedAppearanceSettingsFields.DATE_FORMAT.name, DateFormat.NORMAL.name)
            .let { DateFormat.valueOf(it) }

    fun setDateFormat(format: DateFormat) =
        settings.set(FeedAppearanceSettingsFields.DATE_FORMAT.name, format.name)

    fun getTimeFormat(): TimeFormat =
        settings.getString(FeedAppearanceSettingsFields.TIME_FORMAT.name, TimeFormat.HOURS_24.name)
            .let { TimeFormat.valueOf(it) }

    fun setTimeFormat(format: TimeFormat) =
        settings.set(FeedAppearanceSettingsFields.TIME_FORMAT.name, format.name)

    fun getFeedOrder(): FeedOrder =
        settings.getString(FeedAppearanceSettingsFields.FEED_ORDER.name, FeedOrder.NEWEST_FIRST.name)
            .let { FeedOrder.valueOf(it) }

    fun setFeedOrder(order: FeedOrder) {
        settings[FeedAppearanceSettingsFields.FEED_ORDER.name] = order.name
    }

    fun getFeedLayout(): FeedLayout =
        settings.getString(FeedAppearanceSettingsFields.FEED_LAYOUT.name, FeedLayout.LIST.name)
            .let { FeedLayout.valueOf(it) }

    fun setFeedLayout(feedLayout: FeedLayout) {
        settings[FeedAppearanceSettingsFields.FEED_LAYOUT.name] = feedLayout.name
        feedLayoutMutableFlow.update { feedLayout }
    }

    private companion object {
        const val DEFAULT_FEED_LIST_FONT_SCALE_FACTOR = 0
    }
}

internal enum class FeedAppearanceSettingsFields {
    FEED_ORDER,
    REMOVE_TITLE_FROM_DESCRIPTION,
    HIDE_DESCRIPTION,
    FEED_LIST_FONT_SCALE_FACTOR,
    HIDE_IMAGES,
    HIDE_DATE,
    LEFT_SWIPE_ACTION,
    RIGHT_SWIPE_ACTION,
    DATE_FORMAT,
    TIME_FORMAT,
    FEED_LAYOUT,
}
