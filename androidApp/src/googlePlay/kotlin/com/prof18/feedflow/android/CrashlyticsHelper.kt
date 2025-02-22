package com.prof18.feedflow.android

import co.touchlab.crashkios.crashlytics.enableCrashlytics
import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

object CrashlyticsHelper {
    fun initCrashlytics() {
        enableCrashlytics()
    }

    fun setCollectionEnabled(enabled: Boolean) {
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    @OptIn(ExperimentalKermitApi::class)
    fun crashReportingLogWriter(): LogWriter = CrashlyticsLogWriter()
}
