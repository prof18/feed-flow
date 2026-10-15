package com.prof18.feedflow.android.addfeed

import com.prof18.feedflow.shared.domain.model.FeedAddedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onSubscription

internal fun observeShareFeedResults(
    results: SharedFlow<FeedAddedState>,
    startRequest: () -> Unit,
): Flow<FeedAddedState> = results.onSubscription { startRequest() }
