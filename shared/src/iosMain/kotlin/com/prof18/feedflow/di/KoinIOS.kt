package com.prof18.feedflow.di

import com.prof.rssparser.Parser
import com.prof.rssparser.build
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.domain.opml.OPMLFeedParser
import com.prof18.feedflow.presentation.BaseViewModel
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.SettingsViewModel
import com.prof18.feedflow.utils.DispatcherProvider
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.definition.Definition
import org.koin.core.instance.InstanceFactory
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module

fun initKoinIos(): KoinApplication = initKoin(
    modules = listOf()
)

internal actual inline fun <reified T: BaseViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>
): Pair<Module, InstanceFactory<T>> = factory(qualifier, definition)

internal actual val platformModule: Module = module {
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

    single {
//        Parser.Builder()
//            .build()
        Parser.build()
    }

    factory {
        OPMLFeedParser(
            dispatcherProvider = get(),
        )
    }
}

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getHomeViewModel() = getKoin().get<HomeViewModel>()
    fun getSettingsViewModel() = getKoin().get<SettingsViewModel>()
}