package com.prof18.feedflow.core.model

data class CloudFeedAndCategorySnapshot(
    val sources: List<ParsedFeedSource>,
    val categories: List<FeedSourceCategory>,
)

fun CloudFeedAndCategorySnapshot.applyCloudFeedAndCategoryChanges(
    changes: List<CloudPendingFeedOrCategoryChange>,
): CloudFeedAndCategorySnapshot {
    val categories = applyCategoryChanges(changes)
    val sourcesById = sources.associateBy { it.id }.toMutableMap()
    changes
        .filter { it.entity == CloudFeedOrCategoryEntity.SOURCE }
        .groupBy { it.id }
        .forEach { (id, edits) ->
            val source = mergeSource(id, sourcesById[id], edits.associateBy { it.field }, categories)
            if (source == null) sourcesById.remove(id) else sourcesById[id] = source
        }
    return CloudFeedAndCategorySnapshot(
        sources = sourcesById.values.map { source ->
            source.copy(category = source.category?.id?.let { categories[it] })
        },
        categories = categories.all,
    )
}

private fun CloudFeedAndCategorySnapshot.applyCategoryChanges(
    changes: List<CloudPendingFeedOrCategoryChange>,
): MergedCategories {
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
    val editedIds = changes.filter { it.entity == CloudFeedOrCategoryEntity.CATEGORY }.map { it.id }.toSet()
    return mergeCategoriesWithSameTitle(categoriesById, editedIds)
}

// Titles are unique, but a local create or rename can pick a title another device used for a different id.
// Fold such categories into one, preferring the one no pending change touched, so sync can't get stuck.
private fun mergeCategoriesWithSameTitle(
    categoriesById: Map<String, FeedSourceCategory>,
    editedIds: Set<String>,
): MergedCategories {
    val replacements = mutableMapOf<String, String>()
    categoriesById.values.groupBy { it.title }.values.forEach { sameTitle ->
        val kept = sameTitle.minWith(compareBy<FeedSourceCategory>({ it.id in editedIds }, { it.id }))
        sameTitle.filter { it.id != kept.id }.forEach { replacements[it.id] = kept.id }
    }
    return MergedCategories(
        byId = categoriesById.filterKeys { it !in replacements },
        replacements = replacements,
    )
}

private fun mergeSource(
    id: String,
    source: ParsedFeedSource?,
    fields: Map<CloudFeedOrCategoryField, CloudPendingFeedOrCategoryChange>,
    categories: MergedCategories,
): ParsedFeedSource? =
    when (fields[CloudFeedOrCategoryField.EXISTS]?.value) {
        "0" -> null
        "1" -> ParsedFeedSource(
            id = id,
            url = requireNotNull(fields[CloudFeedOrCategoryField.URL]?.value),
            title = requireNotNull(fields[CloudFeedOrCategoryField.TITLE]?.value),
            category = fields[CloudFeedOrCategoryField.CATEGORY]?.value?.let { categories[it] },
            logoUrl = fields[CloudFeedOrCategoryField.LOGO]?.value,
            websiteUrl = source?.websiteUrl,
        )
        else -> source?.copy(
            url = fields[CloudFeedOrCategoryField.URL]?.let { requireNotNull(it.value) }
                ?: source.url,
            title = fields[CloudFeedOrCategoryField.TITLE]?.let { requireNotNull(it.value) }
                ?: source.title,
            category = if (CloudFeedOrCategoryField.CATEGORY in fields) {
                fields[CloudFeedOrCategoryField.CATEGORY]?.value?.let { categories[it] }
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

private class MergedCategories(
    private val byId: Map<String, FeedSourceCategory>,
    private val replacements: Map<String, String>,
) {
    val all: List<FeedSourceCategory> = byId.values.toList()

    operator fun get(id: String): FeedSourceCategory? = byId[replacements[id] ?: id]
}
