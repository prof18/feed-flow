package com.prof18.feedflow.di

import co.touchlab.kermit.Logger
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.domain.DateFormatter
import com.prof18.feedflow.domain.HtmlParser
import com.prof18.feedflow.domain.IosDateFormatter
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.presentation.BaseViewModel
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.utils.AppEnvironment
import com.prof18.feedflow.utils.DispatcherProvider
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.build
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module

fun initKoinIos(
    htmlParser: HtmlParser,
    appEnvironment: AppEnvironment,
): KoinApplication = initKoin(
    appEnvironment = appEnvironment,
    modules = listOf(
        module {
            factory { htmlParser }
        },
    ),
)

internal actual inline fun <reified T : BaseViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>,
): KoinDefinition<T> = single(qualifier, definition = definition)

@OptIn(ExperimentalSettingsImplementation::class)
internal actual val platformModule: Module = module {
    single<SqlDriver> {
        NativeSqliteDriver(FeedFlowDB.Schema, DatabaseHelper.DATABASE_NAME)
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    single {
        RssParser.build()
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    single<Settings> {
        KeychainSettings(service = "FeedFlow")
    }

    single<DateFormatter> {
        IosDateFormatter(
            logger = getWith("DateFormatter"),
        )
    }
}

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getHomeViewModel() = getKoin().get<HomeViewModel>()
    fun getSettingsViewModel() = getKoin().get<SettingsViewModel>()
    fun getFeedSourceListViewModel() = getKoin().get<FeedSourceListViewModel>()
    fun getAddFeedViewModel() = getKoin().get<AddFeedViewModel>()
    fun getFeedManagerRepository() = getKoin().get<FeedManagerRepository>()
    fun getLogger(tag: String? = null) = getKoin().get<Logger> { parametersOf(tag) }
}
