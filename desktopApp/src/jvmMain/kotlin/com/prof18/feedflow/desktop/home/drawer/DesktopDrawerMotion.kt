package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

private val SidebarExpansionSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntSize.VisibilityThreshold,
)

internal val SidebarExpansionEnter = expandVertically(
    animationSpec = SidebarExpansionSpec,
    expandFrom = Alignment.Top,
)
internal val SidebarExpansionExit = shrinkVertically(
    animationSpec = SidebarExpansionSpec,
    shrinkTowards = Alignment.Top,
)

private val SidebarPlacementSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

// Keep animateItem attached and only drop its placement spec while a category expands or collapses.
// Detaching the modifier resets the item's position history, so rows below a collapsed category
// would re-enter from the bottom of the viewport once the collapse finishes.
internal fun LazyItemScope.sidebarPlacementModifier(enabled: Boolean): Modifier = Modifier.animateItem(
    fadeInSpec = null,
    placementSpec = if (enabled) SidebarPlacementSpec else null,
    fadeOutSpec = null,
)
