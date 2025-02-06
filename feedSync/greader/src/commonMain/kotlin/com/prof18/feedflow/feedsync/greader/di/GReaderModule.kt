package com.prof18.feedflow.feedsync.greader.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.greader.GReaderClient
import com.prof18.feedflow.feedsync.greader.GReaderRepository
import com.prof18.feedflow.feedsync.greader.domain.mapping.ItemContentDTOMapper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
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
        )
    }

    single {
        NetworkSettings(
            settings = get(),
        )
    }
}
