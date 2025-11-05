package com.prof18.feedflow.feedsync.nextcloud.di

import com.prof18.feedflow.feedsync.nextcloud.NextcloudClient
import com.prof18.feedflow.feedsync.nextcloud.NextcloudDataSource
import com.prof18.feedflow.feedsync.nextcloud.NextcloudDataSourceImpl
import com.prof18.feedflow.feedsync.nextcloud.NextcloudFileOperations
import com.prof18.feedflow.feedsync.nextcloud.NextcloudFileOperationsImpl
import com.prof18.feedflow.feedsync.nextcloud.NextcloudSettings
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val nextcloudModule = module {
    single { NextcloudSettings(get()) }
    singleOf(::NextcloudClient)
    singleOf(::NextcloudFileOperationsImpl) bind NextcloudFileOperations::class
    singleOf(::NextcloudDataSourceImpl) bind NextcloudDataSource::class
}
