package com.prof18.feedflow.android.audio

data class AudioEpisode(
    val itemId: String,
    val url: String,
    val title: String,
    val feedName: String?,
    val artworkUrl: String?,
    val kind: AudioSourceKind = AudioSourceKind.PODCAST,
    val contentKey: String? = null,
) {
    val playbackId: String
        get() = if (kind == AudioSourceKind.SPEECH) "speech:$itemId:$contentKey" else itemId
}

data class AudioPlaybackState(
    val episode: AudioEpisode? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isPreparingSpeech: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val canSeek: Boolean = false,
    val failed: Boolean = false,
    val playbackSpeed: AudioPlaybackSpeed = AudioPlaybackSpeed.NORMAL,
)

fun AudioPlaybackState.isPlayingItem(itemId: String): Boolean =
    episode?.kind == AudioSourceKind.PODCAST && episode.itemId == itemId && isPlaying
