package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.home.drawer.DesktopDrawer
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import org.jetbrains.jewel.foundation.theme.JewelTheme

@Composable
internal fun FeedSidebar(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize()
            .background(JewelTheme.globalColors.toolwindowBackground)
            .padding(12.dp),
    ) {
        DesktopDrawer(
            displayState = displayState,
            feedManagementActions = feedManagementActions,
            onFeedFilterSelected = onFeedFilterSelected,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp),
        )
    }
}
