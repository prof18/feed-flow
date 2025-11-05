package com.prof18.feedflow.feedsync.lan.di

import com.prof18.feedflow.feedsync.lan.LanDiscoveryService
import com.prof18.feedflow.feedsync.lan.LanSyncClient
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import com.prof18.feedflow.feedsync.lan.LanSyncSettings
import org.koin.core.module.Module
import org.koin.dsl.module

fun lanSyncModule() = module {
    single { LanSyncSettings(get()) }
    single { LanSyncClient(get()) }

    includes(platformLanModule())
}

expect fun platformLanModule(): Module
