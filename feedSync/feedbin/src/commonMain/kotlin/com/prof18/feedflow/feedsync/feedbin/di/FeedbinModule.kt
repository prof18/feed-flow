package com.prof18.feedflow.feedsync.feedbin.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.feedbin.data.FeedbinClient
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.feedbin.domain.mapping.EntryDTOMapper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

fun getFeedbinModule(appEnvironment: AppEnvironment) = module {
    single {
        FeedbinRepository(
            feedbinClient = FeedbinClient(
                logger = get(parameters = { parametersOf("FeedbinClient") }),
                networkSettings = get(),
                appEnvironment = appEnvironment,
                dispatcherProvider = get(),
            ),
            logger = get(parameters = { parametersOf("FeedbinRepository") }),
            networkSettings = get(),
            databaseHelper = get(),
            entryDTOMapper = EntryDTOMapper(
                htmlParser = get(),
                dateFormatter = get(),
            ),
            dateFormatter = get(),
            dispatcherProvider = get(),
        )
    }

    single {
        NetworkSettings(
            settings = get(),
        )
    }
}

/**
 * Creates a Koin module that provides FeedbinRepository with a custom HTTP client.
 * This is intended for testing purposes where you want to inject a mock HTTP client.
 */
fun getFeedbinTestModule(
    httpClient: HttpClient,
): Module = module {
    single<FeedbinRepository> {
        FeedbinRepository(
            feedbinClient = FeedbinClient(
                logger = get(parameters = { parametersOf("FeedbinClient") }),
                networkSettings = get(),
                appEnvironment = AppEnvironment.Debug,
                dispatcherProvider = get(),
                providedHttpClient = httpClient,
            ),
            logger = get(parameters = { parametersOf("FeedbinRepository") }),
            networkSettings = get(),
            databaseHelper = get(),
            entryDTOMapper = EntryDTOMapper(
                htmlParser = get(),
                dateFormatter = get(),
            ),
            dateFormatter = get(),
            dispatcherProvider = get(),
        )
    }
}
