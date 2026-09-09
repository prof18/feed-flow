package com.prof18.feedflow.core.model

data class CloudPendingFeedOrCategoryChange(
    val entity: CloudFeedOrCategoryEntity,
    val id: String,
    val field: CloudFeedOrCategoryField,
    val value: String?,
    val revision: Long,
)

enum class CloudFeedOrCategoryEntity {
    SOURCE,
    CATEGORY,
}

enum class CloudFeedOrCategoryField {
    EXISTS,
    URL,
    TITLE,
    CATEGORY,
    LOGO,
}
