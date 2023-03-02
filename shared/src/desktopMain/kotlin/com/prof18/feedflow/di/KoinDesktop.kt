package com.prof18.feedflow.di

import com.prof18.feedflow.initDatabase
import com.squareup.sqldelight.db.SqlDriver
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoinDesktop(): KoinApplication = initKoin(
    modules = listOf()
)

actual val platformModule: Module = module {
    single<SqlDriver> {
        initDatabase()
    }
}
