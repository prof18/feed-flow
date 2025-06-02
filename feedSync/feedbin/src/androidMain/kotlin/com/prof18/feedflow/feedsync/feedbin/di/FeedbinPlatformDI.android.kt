package com.prof18.feedflow.feedsync.feedbin.di

import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun ktorEngineModule(): Module = module {
    single { OkHttp.create() } // Ktor engine for Android
}

actual fun provideUsername(): String? {
    // TODO: Implement actual logic to retrieve username from secure storage on Android
    return null 
}

actual fun providePassword(): String? {
    // TODO: Implement actual logic to retrieve password from secure storage on Android
    return null
}
