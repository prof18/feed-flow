package com.prof18.feedflow.shared.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.JvmHtmlParser
import com.prof18.feedflow.shared.domain.JvmHtmlRetriever
import com.prof18.feedflow.shared.domain.ReaderModeExtractor
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.presentation.BaseViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.utils.DispatcherProvider
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel as koinViewModel

internal actual inline fun <reified T : BaseViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>,
): KoinDefinition<T> = koinViewModel(qualifier, definition)

internal actual val platformModule: Module = module {
    single<SqlDriver> {
        createDatabaseDriver(
            context = get(),
        )
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    factory<HtmlParser> {
        JvmHtmlParser(
            logger = getWith("JvmHtmlParser"),
        )
    }

    single<Settings> {
        val sharedPrefs = get<Context>().getSharedPreferences("feedflow.shared.pref", Context.MODE_PRIVATE)
        SharedPreferencesSettings(sharedPrefs)
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    factory<HtmlRetriever> {
        JvmHtmlRetriever(
            dispatcherProvider = get(),
            logger = getWith("JvmHtmlRetriever"),
        )
    }

    factory {
        ReaderModeExtractor(
            dispatcherProvider = get(),
            htmlRetriever = get(),
        )
    }

    viewModel {
        ReaderModeViewModel(
            readerModeExtractor = get(),
        )
    }
}
