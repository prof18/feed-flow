package com.prof18.feedflow.feedsync.feedbin.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.feedbin.data.FeedbinClient
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.feedbin.domain.mapping.EntryDTOMapper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
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
