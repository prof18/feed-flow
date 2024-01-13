package com.prof18.feedflow.shared.presentation.model

internal sealed interface ErrorState

internal data class FeedErrorState(
    val failingSourceName: String,
) : ErrorState

internal data object DatabaseError : ErrorState
