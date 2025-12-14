package com.prof18.feedflow.core.model

// TODO: maybe delete isExpanded and header
data class CategoriesState(
    val isExpanded: Boolean = false,
    val header: String? = null,
    val categories: List<CategoryItem> = listOf(),
    val isLoading: Boolean = false,
) {

    data class CategoryItem(
        val id: String,
        val name: String?,
        val isSelected: Boolean,
    )
}
