package com.prof18.feedflow.android.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.prof18.feedflow.android.MainActivity
import org.koin.android.ext.android.inject

@androidx.annotation.OptIn(UnstableApi::class)
class AudioPlaybackService : MediaSessionService() {
    private val audioPlayer by inject<AndroidAudioPlayer>()
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        val activity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(activity).build().also(::addSession)
        audioPlayer.attach(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        session?.player?.let(audioPlayer::attach)
        if (audioPlayer.state.value.episode == null) stopSelf()
        return result
    }

    override fun onDestroy() {
        session?.player?.let(audioPlayer::detach)
        session?.player?.release()
        session?.release()
        session = null
        super.onDestroy()
    }
}
