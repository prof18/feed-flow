package com.prof18.feedflow.core.model

data class CloudFeedAndCategorySnapshot(
    val sources: List<ParsedFeedSource>,
    val categories: List<FeedSourceCategory>,
)

fun CloudFeedAndCategorySnapshot.applyCloudFeedAndCategoryChanges(
    changes: List<CloudPendingFeedOrCategoryChange>,
): CloudFeedAndCategorySnapshot {
    val categoriesById = applyCategoryChanges(changes)
    val sourcesById = sources.associateBy { it.id }.toMutableMap()
    changes
        .filter { it.entity == CloudFeedOrCategoryEntity.SOURCE }
        .groupBy { it.id }
        .forEach { (id, edits) ->
            val source = mergeSource(id, sourcesById[id], edits.associateBy { it.field }, categoriesById)
            if (source == null) sourcesById.remove(id) else sourcesById[id] = source
        }
    return CloudFeedAndCategorySnapshot(
        sources = sourcesById.values.map { source ->
            source.copy(category = source.category?.id?.let { categoriesById[it] })
        },
        categories = categoriesById.values.toList(),
    )
}

private fun CloudFeedAndCategorySnapshot.applyCategoryChanges(
    changes: List<CloudPendingFeedOrCategoryChange>,
): Map<String, FeedSourceCategory> {
    val categoriesById = categories.associateBy { it.id }.toMutableMap()
    changes
        .filter { it.entity == CloudFeedOrCategoryEntity.CATEGORY }
        .groupBy { it.id }
        .forEach { (id, edits) ->
            val fields = edits.associateBy { it.field }
            when (fields[CloudFeedOrCategoryField.EXISTS]?.value) {
                "0" -> categoriesById.remove(id)
                "1" -> categoriesById[id] = FeedSourceCategory(
                    id = id,
                    title = requireNotNull(fields[CloudFeedOrCategoryField.TITLE]?.value),
                )
                else -> {
                    val category = categoriesById[id]
                    val title = fields[CloudFeedOrCategoryField.TITLE]
                    if (category != null && title != null) {
                        categoriesById[id] = category.copy(title = requireNotNull(title.value))
                    }
                }
            }
        }
    check(categoriesById.values.map { it.title }.distinct().size == categoriesById.size) {
        "Cloud category names conflict"
    }
    return categoriesById
}

private fun mergeSource(
    id: String,
    source: ParsedFeedSource?,
    fields: Map<CloudFeedOrCategoryField, CloudPendingFeedOrCategoryChange>,
    categoriesById: Map<String, FeedSourceCategory>,
): ParsedFeedSource? =
    when (fields[CloudFeedOrCategoryField.EXISTS]?.value) {
        "0" -> null
        "1" -> ParsedFeedSource(
            id = id,
            url = requireNotNull(fields[CloudFeedOrCategoryField.URL]?.value),
            title = requireNotNull(fields[CloudFeedOrCategoryField.TITLE]?.value),
            category = fields[CloudFeedOrCategoryField.CATEGORY]?.value?.let { categoriesById[it] },
            logoUrl = fields[CloudFeedOrCategoryField.LOGO]?.value,
            websiteUrl = source?.websiteUrl,
        )
        else -> source?.copy(
            url = fields[CloudFeedOrCategoryField.URL]?.let { requireNotNull(it.value) }
                ?: source.url,
            title = fields[CloudFeedOrCategoryField.TITLE]?.let { requireNotNull(it.value) }
                ?: source.title,
            category = if (CloudFeedOrCategoryField.CATEGORY in fields) {
                fields[CloudFeedOrCategoryField.CATEGORY]?.value?.let { categoriesById[it] }
            } else {
                source.category
            },
            logoUrl = if (CloudFeedOrCategoryField.LOGO in fields) {
                fields[CloudFeedOrCategoryField.LOGO]?.value
            } else {
                source.logoUrl
            },
        )
    }
