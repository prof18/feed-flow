package com.prof18.feedflow.core.model

data class CategoriesState(
    val categories: List<CategoryItem> = listOf(),
    val isLoading: Boolean = false,
) {

    data class CategoryItem(
        val id: String,
        val name: String?,
        val isSelected: Boolean,
    )
}
