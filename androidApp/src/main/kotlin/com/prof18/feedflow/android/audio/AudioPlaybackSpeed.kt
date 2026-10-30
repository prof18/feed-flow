package com.prof18.feedflow.android.audio

import java.text.NumberFormat

enum class AudioPlaybackSpeed(val rate: Float, val labelValue: String) {
    HALF(rate = 0.5f, labelValue = "0.5"),
    NORMAL(rate = 1f, labelValue = "1"),
    ONE_AND_HALF(rate = 1.5f, labelValue = "1.5"),
    DOUBLE(rate = 2f, labelValue = "2"),
    ;

    fun formattedLabel(): String = NumberFormat.getNumberInstance().run {
        maximumFractionDigits = 1
        "${format(rate)}×"
    }
}
