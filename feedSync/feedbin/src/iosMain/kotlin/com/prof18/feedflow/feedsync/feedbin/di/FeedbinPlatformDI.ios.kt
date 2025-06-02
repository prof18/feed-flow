package com.prof18.feedflow.feedsync.feedbin.di

import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun ktorEngineModule(): Module = module {
    single { Darwin.create() } // Ktor engine for iOS
}

actual fun provideUsername(): String? {
    // TODO: Implement actual logic to retrieve username from secure storage on iOS (e.g., Keychain)
    return null
}

actual fun providePassword(): String? {
    // TODO: Implement actual logic to retrieve password from secure storage on iOS (e.g., Keychain)
    return null
}
