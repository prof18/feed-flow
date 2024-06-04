package com.prof18.feedflow.feedsync.database.di

import com.prof18.feedflow.core.utils.AppEnvironment
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    scope<FeedSyncScope> {
        scoped {
            createDatabaseDriver(
                appEnvironment = appEnvironment,
                logger = get(parameters = { parametersOf("initDatabase") }),
            )
        }
    }
}
