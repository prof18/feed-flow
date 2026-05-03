package com.prof18.feedflow.core.model

data class CategoryName(
    val name: String,
)

enum class CategoryNameValidationResult {
    VALID,
    BLANK,
    DUPLICATE,
}

fun CategoryName.trimmed(): CategoryName =
    CategoryName(name = name.trim())

fun CategoryName.canonical(): String =
    name.canonicalCategoryName()

fun String.canonicalCategoryName(): String =
    trim().lowercase()
