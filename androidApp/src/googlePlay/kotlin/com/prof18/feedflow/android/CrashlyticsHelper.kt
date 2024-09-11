package com.prof18.feedflow.android

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.prof18.feedflow.shared.utils.enableKmpCrashlytics

class CrashlyticsHelper {
    fun initCrashlytics() {
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
        enableKmpCrashlytics()
    }
}
