package com.prof18.feedflow.di

import com.prof.rssparser.Parser
import com.prof.rssparser.build
import com.prof18.feedflow.initDatabase
import com.prof18.feedflow.utils.DispatcherProvider
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

    single {
//        Parser.Builder()
//            .build()
        Parser.build()
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }
}
