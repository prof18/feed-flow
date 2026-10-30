package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState

sealed interface SearchRequestState {
    data object Idle : SearchRequestState
    data class Loading(val request: SearchRequest) : SearchRequestState
    data class Results(val request: SearchRequest, val result: SearchState) : SearchRequestState
    data class Error(val request: SearchRequest) : SearchRequestState
}

data class SearchRequest(
    val generation: Long,
    val query: String,
    val filter: SearchFilter,
    val feedFilter: FeedFilter?,
)
