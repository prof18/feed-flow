package com.prof18.feedflow.utils

import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin
import co.touchlab.crashkios.crashlytics.enableCrashlytics
import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook

fun setupCrashlytics() {
    enableCrashlytics()
    setCrashlyticsUnhandledExceptionHook()
}

fun sendCrash() {
    CrashlyticsKotlin.sendHandledException(Exception("Some exception"))
}
