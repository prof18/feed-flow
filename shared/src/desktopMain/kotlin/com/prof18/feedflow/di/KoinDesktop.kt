package com.prof18.feedflow.di

import com.prof18.feedflow.domain.DateFormatter
import com.prof18.feedflow.domain.HtmlParser
import com.prof18.feedflow.domain.JvmAndroidDateFormatter
import com.prof18.feedflow.domain.JvmHtmlParser
import com.prof18.feedflow.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.initDatabase
import com.prof18.feedflow.presentation.BaseViewModel
import com.prof18.feedflow.utils.AppEnvironment
import com.prof18.feedflow.utils.DispatcherProvider
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import java.util.prefs.Preferences

fun initKoinDesktop(
    appEnvironment: AppEnvironment,
): KoinApplication = initKoin(
    appEnvironment = appEnvironment,
    modules = listOf(),
)

internal actual inline fun <reified T : BaseViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>,
): KoinDefinition<T> = factory(qualifier, definition)

internal actual val platformModule: Module = module {
    single<SqlDriver> {
        initDatabase(
            logger = getWith("initDatabase"),
        )
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    factory<HtmlParser> {
        JvmHtmlParser()
    }

    single<Settings> {
        val preferences = Preferences.userRoot()
        PreferencesSettings(preferences)
    }

    single<DateFormatter> {
        JvmAndroidDateFormatter(
            logger = getWith("DateFormatter"),
        )
    }
}
