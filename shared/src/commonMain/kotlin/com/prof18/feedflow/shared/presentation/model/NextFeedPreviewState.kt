package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.FeedFilter

sealed class NextFeedPreviewState {
    data class NextFeedPreviewEnabledState(
        val title: String,
        val feedFilter: FeedFilter,
    ) : NextFeedPreviewState()

    data object NextFeedPreviewDisabledState : NextFeedPreviewState()
}
