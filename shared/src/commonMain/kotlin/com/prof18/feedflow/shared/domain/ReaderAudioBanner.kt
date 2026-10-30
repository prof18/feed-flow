package com.prof18.feedflow.shared.domain

data class ReaderAudioBanner(
    val title: String?,
    val audioLabel: String,
    val openAudioLabel: String,
    val untitledAudioLabel: String,
    val enablePlayback: Boolean = false,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val playAudioLabel: String = "",
    val pauseAudioLabel: String = "",
)
