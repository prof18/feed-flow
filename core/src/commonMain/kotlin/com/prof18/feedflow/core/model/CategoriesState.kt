package com.prof18.feedflow.core.model

data class CategoriesState(
    val isExpanded: Boolean = false,
    val header: String? = null,
    val categories: List<CategoryItem> = listOf(),
) {

    data class CategoryItem(
        val id: String,
        val name: String?,
        val isSelected: Boolean,
        val onClick: (CategoryId) -> Unit,
    )
}
