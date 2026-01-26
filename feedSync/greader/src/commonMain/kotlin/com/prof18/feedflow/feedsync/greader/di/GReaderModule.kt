package com.prof18.feedflow.feedsync.greader.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.greader.data.GReaderClient
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.feedsync.greader.domain.mapping.ItemContentDTOMapper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

fun getGReaderModule(appEnvironment: AppEnvironment) = module {
    single {
        GReaderRepository(
            gReaderClient = GReaderClient(
                logger = get(parameters = { parametersOf("GReaderClient") }),
                networkSettings = get(),
                appEnvironment = appEnvironment,
                dispatcherProvider = get(),
            ),
            logger = get(parameters = { parametersOf("GReaderRepository") }),
            networkSettings = get(),
            databaseHelper = get(),
            itemContentDTOMapper = ItemContentDTOMapper(
                htmlParser = get(),
                dateFormatter = get(),
            ),
            dateFormatter = get(),
            dispatcherProvider = get(),
            feedSourceLogoRetriever = get(),
        )
    }

    single {
        NetworkSettings(
            settings = get(),
        )
    }
}

/**
 * Creates a Koin module that provides GReaderRepository with a custom HTTP client.
 * This is intended for testing purposes where you want to inject a mock HTTP client.
 */
fun getGReaderTestModule(
    httpClient: HttpClient,
): Module = module {
    single<GReaderRepository> {
        GReaderRepository(
            gReaderClient = GReaderClient(
                logger = get(parameters = { parametersOf("GReaderClient") }),
                networkSettings = get(),
                appEnvironment = AppEnvironment.Debug,
                dispatcherProvider = get(),
                providedHttpClient = httpClient,
            ),
            logger = get(parameters = { parametersOf("GReaderRepository") }),
            networkSettings = get(),
            databaseHelper = get(),
            itemContentDTOMapper = ItemContentDTOMapper(
                htmlParser = get(),
                dateFormatter = get(),
            ),
            dateFormatter = get(),
            dispatcherProvider = get(),
            feedSourceLogoRetriever = get(),
        )
    }
}
