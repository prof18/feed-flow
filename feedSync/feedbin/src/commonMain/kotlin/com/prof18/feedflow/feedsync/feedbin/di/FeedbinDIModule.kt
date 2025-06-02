package com.prof18.feedflow.feedsync.feedbin.di

import com.prof18.feedflow.feedsync.feedbin.remote.FeedbinApi
import com.prof18.feedflow.feedsync.feedbin.remote.FeedbinApiClient
import com.prof18.feedflow.feedsync.feedbin.service.FeedbinServiceImpl
import io.ktor.client.HttpClient
// Auth plugins are not directly used in client constructor if basicAuth is applied per-request
// import io.ktor.client.plugins.auth.Auth
// import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
// import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Expect actuals for platform-specific parts
expect fun ktorEngineModule(): Module
expect fun provideUsername(): String?
expect fun providePassword(): String?

// Named qualifier for Feedbin components
val feedbinServiceName = named("FeedbinSyncService") // Matches the plan
val feedbinHttpClientName = named("FeedbinHttpClient") // Specific HttpClient for Feedbin

val feedbinModule = module {
    includes(ktorEngineModule()) // Includes platform-specific Ktor engine (e.g., OkHttp, Darwin)

    single<HttpClient>(feedbinHttpClientName) {
        HttpClient(get()) { // get() resolves the Ktor engine from ktorEngineModule()
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true 
                    prettyPrint = false // Usually false for production, true for debugging
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT // Or a Kermit logger
                level = LogLevel.INFO // Or LogLevel.HEADERS / LogLevel.BODY for more detail
                // filter { request -> request.url.host.contains("api.feedbin.com") } // Optional: filter logs
            }
            install(HttpRequestRetry) {
                maxRetries = 3 // Total max retries for any condition below
                retryIf { _, response ->
                    response.status.value.let { it == 429 || it >= 500 } // Retry on 429 or 5xx
                }
                exponentialDelay()
                // Log retries using the Ktor logger if desired
                // delayMillis { retry -> LogLevel.INFO; 3000L * retry } // Example custom delay
            }
            // The Auth plugin for Ktor's HttpClient can be set up here if credentials
            // were static for the client's lifetime. However, FeedbinApiClient uses
            // per-request basicAuth extension via usernameManager/passwordManager,
            // which is more flexible if credentials can change or are loaded lazily.
            // If a global Auth setup is preferred and credentials are known when HttpClient is created:
            // install(Auth) {
            //     basic {
            //         credentials {
            //             BasicAuthCredentials(username = provideUsername() ?: "", password = providePassword() ?: "")
            //         }
            //         sendWithoutRequest { request ->
            //             // Configure when to send auth header (e.g., only for api.feedbin.com)
            //             request.url.host == "api.feedbin.com"
            //         }
            //     }
            // }
        }
    }

    factory<FeedbinApi> { // factory because it depends on username/password which might change/be runtime-dependent
        FeedbinApiClient(
            httpClient = get(feedbinHttpClientName), // Get the named HttpClient
            usernameManager = ::provideUsername,
            passwordManager = ::providePassword
        )
    }

    // Using 'factory' for FeedbinServiceImpl as well, in case its dependencies (like FeedbinApi)
    // or its own state needs to be fresh. If it's stateless and its deps are singletons,
    // it could be a 'single'. Given GReaderRepository is often a singleton, this might change.
    // For now, 'factory' aligns with FeedbinApi being a factory.
    factory<FeedbinServiceImpl>(feedbinServiceName) { // Provide FeedbinServiceImpl, not FeedSyncService directly yet
        FeedbinServiceImpl(
            feedbinApi = get() // Koin resolves FeedbinApi
            // TODO: Inject databaseHelper, networkSettings, logger if FeedbinServiceImpl needs them
            // databaseHelper = get(),
            // networkSettings = get(),
            // logger = get() 
        )
    }
    
    // If FeedbinServiceImpl is meant to be one of the implementations for a common FeedSyncService:
    // factory<FeedSyncService>(feedbinServiceName) { // Qualify the FeedSyncService binding
    //     FeedbinServiceImpl(
    //         feedbinApi = get(),
    //         // ... other dependencies
    //     )
    // }
}
