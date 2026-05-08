package com.prof18.feedflow.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Browser(
    val id: String,
    val name: String,
    val isFavourite: Boolean = false,
)
