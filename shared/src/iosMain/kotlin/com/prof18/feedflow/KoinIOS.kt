package com.prof18.feedflow

import com.prof18.feedflow.db.FeedFlowDB
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoinIos(): KoinApplication = initKoin(
    modules = listOf()
)

actual val platformModule: Module = module {
    single<SqlDriver> {
        NativeSqliteDriver(FeedFlowDB.Schema, DatabaseHelper.DATABASE_NAME)
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.Default
        }
    }
}