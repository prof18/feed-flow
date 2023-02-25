package com.prof18.feedflow

sealed interface ErrorState

data class FeedErrorState(
    val failingSourceName: String,
): ErrorState
