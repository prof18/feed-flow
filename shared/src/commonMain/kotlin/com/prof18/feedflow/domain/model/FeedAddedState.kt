package com.prof18.feedflow.domain.model

import dev.icerock.moko.resources.desc.StringDesc

sealed class FeedAddedState {
    data object FeedNotAdded : FeedAddedState()
    data class FeedAdded(
        val message: StringDesc,
    ) : FeedAddedState()

    data class Error(
        val errorMessage: StringDesc,
    ) : FeedAddedState()
}
