package com.prof18.feedflow.shared.di

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.feedflow.shared.domain.browser.BrowserSettingsRepository
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.BaseViewModel
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.utils.DispatcherProvider
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
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
    languageCode: String,
): KoinApplication = initKoin(
    appEnvironment = appEnvironment,
    modules = listOf(
        module {
            factory { htmlParser }
            single<FeedFlowStrings> {
                feedFlowStrings[languageCode] ?: EnFeedFlowStrings
            }
        },
    ),
)

internal actual inline fun <reified T : BaseViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>,
): KoinDefinition<T> = single(qualifier, definition = definition)

@OptIn(ExperimentalSettingsImplementation::class)
internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    single<SqlDriver> {
        createDatabaseDriver(appEnvironment)
    }

    single<DispatcherProvider> {
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val default: CoroutineDispatcher = Dispatchers.Default
            override val io: CoroutineDispatcher = Dispatchers.IO
        }
    }

    factory {
        OpmlFeedHandler(
            dispatcherProvider = get(),
        )
    }

    single<Settings> {
        KeychainSettings(service = "FeedFlow")
    }
}

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getHomeViewModel() = getKoin().get<HomeViewModel>()
    fun getFeedSourceListViewModel() = getKoin().get<FeedSourceListViewModel>()
    fun getAddFeedViewModel() = getKoin().get<AddFeedViewModel>()
    fun getBrowserSettingsRepository() = getKoin().get<BrowserSettingsRepository>()
    fun getLogger(tag: String? = null) = getKoin().get<Logger> { parametersOf(tag) }
    fun getImportExportViewModel() = getKoin().get<ImportExportViewModel>()
    fun getSettingsViewModel() = getKoin().get<SettingsViewModel>()
    fun getFeedFlowStrings() = getKoin().get<FeedFlowStrings>()
    fun getSettingsRepository() = getKoin().get<SettingsRepository>()
    fun getSearchViewModel() = getKoin().get<SearchViewModel>()
}
