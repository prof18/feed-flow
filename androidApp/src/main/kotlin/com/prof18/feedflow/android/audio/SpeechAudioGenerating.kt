package com.prof18.feedflow.android.audio

import java.io.File

interface SpeechAudioGenerating {
    suspend fun generate(segments: List<String>): File
}
