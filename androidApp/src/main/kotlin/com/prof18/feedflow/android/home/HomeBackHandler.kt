package com.prof18.feedflow.android.home

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import com.prof18.feedflow.core.model.FeedFilter

/**
 * Makes system Back step out of the home screen one layer at a time: an open modal drawer closes
 * first, then a feed, category or other filter returns to Timeline. Only the Timeline root lets
 * Back reach the activity and leave the app.
 *
 * The filter lives in shared state rather than on the navigation back stack, so without this
 * handler Back from a filter would skip straight past Timeline.
 */
@Composable
internal fun HomeBackHandler(
    isDrawerOpen: Boolean,
    currentFeedFilter: FeedFilter,
    onCloseDrawer: () -> Unit,
    onShowTimeline: () -> Unit,
) {
    val isTimeline = currentFeedFilter is FeedFilter.Timeline
    BackHandler(enabled = isDrawerOpen || !isTimeline) {
        if (isDrawerOpen) {
            onCloseDrawer()
        } else {
            onShowTimeline()
        }
    }
}

/**
 * Whether Back should treat the modal drawer as the topmost surface. [DrawerState.isOpen] stays
 * false until the opening animation settles, so Back pressed mid-animation would skip the drawer;
 * the target value flips as soon as the drawer starts moving. A drawer already closing counts as
 * closed, so a second Back during that animation steps on to the filter.
 */
internal val DrawerState.isOpenForBack: Boolean
    get() = targetValue == DrawerValue.Open
