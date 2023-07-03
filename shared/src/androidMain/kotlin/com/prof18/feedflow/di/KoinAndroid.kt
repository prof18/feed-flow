package com.prof18.feedflow.di

import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.domain.HtmlParser
import com.prof18.feedflow.domain.JvmHtmlParser
import com.prof18.feedflow.domain.opml.OPMLFeedParser
import com.prof18.feedflow.presentation.BaseViewModel
import com.prof18.feedflow.utils.DispatcherProvider
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.build
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.definition.Definition
import org.koin.core.instance.InstanceFactory
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel as koinViewModel

internal actual inline fun <reified T: BaseViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>
): Pair<Module, InstanceFactory<T>> = koinViewModel(qualifier, definition)

internal actual val platformModule: Module = module {
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

    single {
        RssParser.build()
    }

    factory<HtmlParser> {
        JvmHtmlParser()
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }
}
