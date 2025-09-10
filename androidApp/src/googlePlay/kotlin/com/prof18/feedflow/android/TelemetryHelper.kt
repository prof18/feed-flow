package com.prof18.feedflow.android

import android.content.Context
import com.telemetrydeck.sdk.TelemetryDeck

object TelemetryHelper {
    fun initTelemetry(applicationContext: Context) {
        val builder = TelemetryDeck.Builder()
            .appID("0334762E-7A84-4A80-A1BA-879165ED0333")

        TelemetryDeck.start(applicationContext, builder)
    }
}
