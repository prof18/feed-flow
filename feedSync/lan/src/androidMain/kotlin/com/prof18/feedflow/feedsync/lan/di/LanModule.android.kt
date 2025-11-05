package com.prof18.feedflow.feedsync.lan.di

import com.prof18.feedflow.feedsync.lan.LanDiscoveryService
import com.prof18.feedflow.feedsync.lan.LanDiscoveryServiceAndroid
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformLanModule(): Module = module {
    single<LanDiscoveryService> {
        LanDiscoveryServiceAndroid(
            context = get(),
            logger = get(),
        )
    }

    single {
        LanSyncServer(
            settings = get(),
            getDatabaseFileBytes = { null }, // Will be provided by the worker
            logger = get(),
        )
    }

    single {
        LanSyncRepository(
            settings = get(),
            discoveryService = get(),
            syncServer = get(),
            syncClient = get(),
            logger = get(),
            saveDatabaseFile = { false }, // Will be provided by the worker
        )
    }
}
