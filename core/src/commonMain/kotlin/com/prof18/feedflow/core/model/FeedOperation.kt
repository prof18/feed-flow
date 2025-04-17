package com.prof18.feedflow.core.model

sealed class FeedOperation {
    data object None : FeedOperation()
    data object Deleting : FeedOperation()
    data object MarkingAllRead : FeedOperation()
}
