package com.prof18.feedflow.core.model

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

data class SuggestedFeedCategory(
    val id: String,
    val name: String,
    val icon: String,
    val feeds: List<SuggestedFeed>,
)

@OptIn(ExperimentalObjCName::class)
data class SuggestedFeed(
    val name: String,
    val url: String,
    // `description` collides with NSObject.description in Swift.
    @ObjCName("feedDescription")
    val description: String,
    val logoUrl: String? = null,
)
