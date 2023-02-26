package com.prof18.feedflow

import com.prof.rssparser.Parser
import com.prof18.feedflow.db.FeedFlowDB
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            FeedFlowDB.Schema,
            get(),
            DatabaseHelper.DATABASE_NAME,
        )
    }

    factory {
        OPMLFeedParser(
            dispatcherProvider = get(),
        )
    }

    factory {
        OPMLImporter(
            context = get(),
            dispatcherProvider = get(),
        )
    }

    factory {
        FeedManagerRepository(
            databaseHelper = get(),
            opmlFeedParser = get(),
        )
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    single {
        Parser.Builder()
            .build()
    }

    single {
        FeedRetrieverRepository(
            parser = get(),
            databaseHelper = get(),
            dispatcherProvider = get(),
        )
    }
}