package com.prof18.feedflow.android.audio

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.prof18.feedflow.android.AudioUrlOpener
import com.prof18.feedflow.core.model.AudioEnclosure
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.shared.domain.audio.AudioPlaybackPositionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

class AndroidAudioPlayer(
    private val context: Context,
    private val positions: AudioPlaybackPositionRepository,
    private val speechGenerator: SpeechAudioGenerating,
    private val speechText: suspend (String?, String) -> List<String> = { _, _ -> error("Speech text not configured") },
) {
    private val mutableState = MutableStateFlow(AudioPlaybackState())
    val state = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private var player: Player? = null
    private var pendingEpisode: AudioEpisode? = null
    private var completed = false
    private var hasValidPosition = false
    private var selectedSpeed = AudioPlaybackSpeed.NORMAL
    private var speechJob: Job? = null
    private var speechGeneration = 0L
    private var speechRequest: SpeechRequest? = null
    private var cachedSpeech: AudioEpisode? = null
    private var speechFile: File? = null

    private var listener: Player.Listener? = null

    private fun playerListener(sourcePlayer: Player): Player.Listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player !== this@AndroidAudioPlayer.player) return
            if (player.currentMediaItem?.mediaId != state.value.episode?.playbackId) return
            if (player.playbackState == Player.STATE_READY) hasValidPosition = true
            if (player.playbackState == Player.STATE_ENDED) {
                completed = true
                state.value.episode?.let { positions.savePosition(it.playbackId, 0) }
            }
            publishState()
            if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                events.contains(Player.EVENT_POSITION_DISCONTINUITY)
            ) {
                savePosition()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (sourcePlayer !== player || state.value.isPreparingSpeech ||
                sourcePlayer.currentMediaItem?.mediaId != state.value.episode?.playbackId
            ) {
                return
            }
            savePosition()
            player?.pause()
            mutableState.value = state.value.copy(isPlaying = false, isLoading = false, canSeek = false, failed = true)
        }
    }

    fun playReaderEpisode(article: ReaderModeData, untitledTitle: String) {
        AudioEnclosure.validatedUrl(article.audioUrl)?.let { audioUrl ->
            playEpisode(
                AudioEpisode(
                    itemId = article.id.id,
                    url = audioUrl,
                    title = article.title?.takeIf { it.isNotBlank() } ?: untitledTitle,
                    feedName = article.siteName,
                    artworkUrl = article.audioImageUrl,
                ),
            )
        }
    }

    fun isSpeechFor(article: ReaderModeData): Boolean {
        val request = speechRequest ?: return false
        return state.value.episode?.kind == AudioSourceKind.SPEECH &&
            request.article.id == article.id && request.article.title == article.title &&
            request.article.content == article.content
    }

    fun playReaderSpeech(article: ReaderModeData, untitledTitle: String) {
        val title = article.title?.takeIf { it.isNotBlank() } ?: untitledTitle
        val source = if (article.title == null) {
            byteArrayOf((-1).toByte()) + "\u0000${article.content}".toByteArray(Charsets.UTF_8)
        } else {
            "${article.title}\u0000${article.content}".toByteArray(Charsets.UTF_8)
        }
        val key = MessageDigest.getInstance("SHA-256")
            .digest(source)
            .joinToString("") { "%02x".format(it) }
        val request = SpeechRequest(article, title, key)
        if (state.value.episode?.playbackId == request.episode.playbackId && !state.value.failed) {
            togglePlayback()
            return
        }
        speechRequest = request
        val cached = cachedSpeech
        if (cached?.playbackId == request.episode.playbackId && speechFile?.isFile == true) {
            cancelSpeechPreparation()
            selectEpisode(cached)
        } else {
            prepareSpeech(request)
        }
    }

    private fun prepareSpeech(request: SpeechRequest) {
        cancelSpeechPreparation()
        savePosition()
        player?.pause()
        player?.clearMediaItems()
        pendingEpisode = null
        completed = false
        hasValidPosition = false
        mutableState.value = AudioPlaybackState(
            episode = request.episode,
            isPlaying = true,
            isLoading = true,
            isPreparingSpeech = true,
            playbackSpeed = selectedSpeed,
        )
        val generation = speechGeneration
        speechJob = scope.launch {
            var output: File? = null
            try {
                val segments = speechText(request.article.title, request.article.content)
                output = speechGenerator.generate(segments)
                if (generation != speechGeneration) return@launch
                val shouldPlay = state.value.isPlaying
                clearSpeechFile()
                speechFile = output
                val episode = request.episode.copy(url = output.toURI().toString())
                cachedSpeech = episode
                output = null
                selectEpisode(episode, shouldPlay)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == speechGeneration) {
                    mutableState.value = state.value.copy(
                        isPlaying = false, isLoading = false, isPreparingSpeech = false, failed = true,
                    )
                }
            } finally {
                output?.let(::deleteSpeechFile)
                if (generation == speechGeneration) speechJob = null
            }
        }
    }

    private fun cancelSpeechPreparation() {
        speechGeneration++
        speechJob?.cancel()
        speechJob = null
    }

    private fun clearSpeechFile() {
        speechFile?.let(::deleteSpeechFile)
        speechFile = null
        cachedSpeech = null
    }

    private fun deleteSpeechFile(file: File) {
        file.delete()
        file.parentFile?.takeIf { it.name.startsWith("reader-speech-") }?.deleteRecursively()
    }

    internal fun openExternal(context: Context, opener: AudioUrlOpener, url: String, failedLabel: String) {
        pause()
        if (!opener.open(url)) Toast.makeText(context, failedLabel, Toast.LENGTH_SHORT).show()
    }

    fun playEpisode(episode: AudioEpisode) {
        if (episode.kind != AudioSourceKind.PODCAST || AudioEnclosure.validatedUrl(episode.url) == null) return
        cancelSpeechPreparation()
        selectEpisode(episode)
    }

    private fun selectEpisode(episode: AudioEpisode, shouldPlay: Boolean = true) {
        if (state.value.episode?.playbackId == episode.playbackId && state.value.episode?.url == episode.url) {
            togglePlayback()
            return
        }
        savePosition()
        completed = false
        hasValidPosition = false
        pendingEpisode = episode
        mutableState.value = AudioPlaybackState(
            episode = episode,
            isPlaying = shouldPlay,
            isLoading = shouldPlay,
            playbackSpeed = selectedSpeed,
        )
        if (player != null) {
            preparePendingEpisode()
        } else {
            startPlaybackService()
        }
    }

    internal fun attach(player: Player) {
        if (this.player === player) return
        listener?.let { this.player?.removeListener(it) }
        ticker?.cancel()
        this.player = player
        val newListener = playerListener(player)
        listener = newListener
        player.addListener(newListener)
        player.setPlaybackSpeed(selectedSpeed.rate)
        preparePendingEpisode()
        ticker = scope.launch {
            var ticks = 0
            while (true) {
                delay(TICK_MS)
                publishState()
                ticks++
                if (ticks % SAVE_TICKS == 0) savePosition()
            }
        }
    }

    internal fun detach(expectedPlayer: Player? = player) {
        if (player !== expectedPlayer) return
        savePosition()
        cancelSpeechPreparation()
        ticker?.cancel()
        ticker = null
        listener?.let { player?.removeListener(it) }
        listener = null
        player?.clearMediaItems()
        player = null
        pendingEpisode = null
        mutableState.value = AudioPlaybackState(playbackSpeed = selectedSpeed)
        clearSpeechFile()
        speechRequest = null
    }

    private fun preparePendingEpisode() {
        val episode = pendingEpisode ?: return
        val currentPlayer = player ?: return
        val shouldPlay = state.value.isPlaying
        pendingEpisode = null
        val media = MediaItem.Builder()
            .setMediaId(episode.playbackId)
            .setUri(episode.url.toUri().normalizeScheme())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(episode.feedName)
                    .setArtworkUri(episode.artworkUrl?.toUri())
                    .build(),
            )
            .build()
        currentPlayer.setMediaItem(media, positions.getPosition(episode.playbackId))
        currentPlayer.prepare()
        if (shouldPlay) currentPlayer.play() else currentPlayer.pause()
    }

    fun togglePlayback() {
        if (state.value.isPreparingSpeech) {
            mutableState.value = state.value.copy(isPlaying = !state.value.isPlaying)
            return
        }
        if (shouldRegenerateSpeech()) {
            speechRequest?.let(::prepareSpeech)
            return
        }
        val currentPlayer = player
        if (currentPlayer == null) {
            val episode = state.value.episode ?: return
            pendingEpisode = episode
            if (state.value.isPlaying) {
                pause()
            } else {
                val retryServiceStart = state.value.failed
                mutableState.value = state.value.copy(isPlaying = true, isLoading = true, failed = false)
                if (retryServiceStart) startPlaybackService()
            }
            return
        }
        if (currentPlayer.currentMediaItem == null) {
            pendingEpisode = state.value.episode
            if (state.value.isPlaying) {
                pause()
            } else {
                mutableState.value = state.value.copy(isPlaying = true, isLoading = true, failed = false)
                preparePendingEpisode()
            }
            return
        }
        if (currentPlayer.playWhenReady && !state.value.failed && !completed) {
            pause()
        } else {
            if (completed) {
                completed = false
                currentPlayer.seekTo(0)
            }
            if (state.value.failed) {
                mutableState.value = state.value.copy(failed = false)
                currentPlayer.prepare()
            }
            currentPlayer.play()
        }
    }

    private fun shouldRegenerateSpeech(): Boolean =
        state.value.failed && state.value.episode?.kind == AudioSourceKind.SPEECH &&
            (state.value.episode?.url.isNullOrBlank() || speechFile?.isFile != true)

    fun pause() {
        mutableState.value = state.value.copy(isPlaying = false, isLoading = state.value.isPreparingSpeech)
        player?.pause()
        savePosition()
    }

    fun setPlaybackSpeed(speed: AudioPlaybackSpeed) {
        selectedSpeed = speed
        mutableState.value = state.value.copy(playbackSpeed = speed)
        player?.setPlaybackSpeed(speed.rate)
    }

    private fun startPlaybackService() {
        try {
            context.startService(Intent(context, AudioPlaybackService::class.java))
        } catch (_: IllegalStateException) {
            mutableState.value = state.value.copy(isPlaying = false, isLoading = false, failed = true)
        } catch (_: SecurityException) {
            mutableState.value = state.value.copy(isPlaying = false, isLoading = false, failed = true)
        }
    }

    fun seek(positionMs: Long) {
        val currentPlayer = player ?: return
        if (!state.value.canSeek) return
        completed = false
        hasValidPosition = true
        currentPlayer.seekTo(positionMs.coerceIn(0, state.value.durationMs))
        savePosition()
        publishState()
    }

    fun close() {
        cancelSpeechPreparation()
        savePosition()
        pendingEpisode = null
        player?.stop()
        player?.clearMediaItems()
        mutableState.value = AudioPlaybackState(playbackSpeed = selectedSpeed)
        detach()
        context.stopService(Intent(context, AudioPlaybackService::class.java))
        clearSpeechFile()
        speechRequest = null
    }

    fun savePosition() {
        val episode = state.value.episode ?: return
        val currentPlayer = player ?: return
        // A replacement may update the UI before the old player's final callback is delivered.
        if (currentPlayer.currentMediaItem?.mediaId != episode.playbackId || !hasValidPosition && !completed) return
        positions.savePosition(
            episode.playbackId,
            if (completed) 0 else currentPlayer.currentPosition,
            currentPlayer.duration.coerceAtLeast(0),
        )
    }

    private fun publishState() {
        if (state.value.isPreparingSpeech) return
        val currentPlayer = player ?: return
        val episode = state.value.episode ?: return
        if (currentPlayer.currentMediaItem?.mediaId != episode.playbackId) return
        val duration = currentPlayer.duration.coerceAtLeast(0)
        mutableState.value = state.value.copy(
            isPlaying = currentPlayer.playWhenReady && currentPlayer.playbackState != Player.STATE_ENDED &&
                currentPlayer.playerError == null && !state.value.failed,
            isLoading = currentPlayer.playbackState == Player.STATE_BUFFERING,
            positionMs = if (completed) 0 else currentPlayer.currentPosition.coerceAtLeast(0),
            durationMs = duration,
            canSeek = currentPlayer.isCurrentMediaItemSeekable && duration > 0,
            failed = currentPlayer.playerError != null || state.value.failed,
        )
    }

    private companion object {
        const val TICK_MS = 1000L
        const val SAVE_TICKS = 5
    }
}

private data class SpeechRequest(val article: ReaderModeData, val title: String, val contentKey: String) {
    val episode: AudioEpisode = AudioEpisode(
        itemId = article.id.id,
        url = "",
        title = title,
        feedName = article.siteName,
        artworkUrl = article.audioImageUrl,
        kind = AudioSourceKind.SPEECH,
        contentKey = contentKey,
    )
}
