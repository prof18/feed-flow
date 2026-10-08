package com.prof18.feedflow.android.audio

data class AudioEpisode(
    val itemId: String,
    val url: String,
    val title: String,
    val feedName: String?,
    val artworkUrl: String?,
)

data class AudioPlaybackState(
    val episode: AudioEpisode? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val canSeek: Boolean = false,
    val failed: Boolean = false,
    val playbackSpeed: AudioPlaybackSpeed = AudioPlaybackSpeed.NORMAL,
)

fun AudioPlaybackState.isPlayingItem(itemId: String): Boolean = episode?.itemId == itemId && isPlaying
