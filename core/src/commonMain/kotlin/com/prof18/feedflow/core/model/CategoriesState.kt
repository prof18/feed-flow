package com.prof18.feedflow.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CategoriesState(
    val categories: ImmutableList<CategoryItem> = persistentListOf(),
    val isLoading: Boolean = false,
) {

    data class CategoryItem(
        val id: String,
        val name: String?,
        val isSelected: Boolean,
    )
}
