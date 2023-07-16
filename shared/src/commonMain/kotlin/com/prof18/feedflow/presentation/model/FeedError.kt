package com.prof18.feedflow.presentation.model

sealed interface ErrorState

data class FeedErrorState(
    val failingSourceName: String,
) : ErrorState
