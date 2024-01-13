package com.prof18.feedflow.shared.domain.model

data class Browser(
    val id: String,
    val name: String,
    val isFavourite: Boolean = false,
)
