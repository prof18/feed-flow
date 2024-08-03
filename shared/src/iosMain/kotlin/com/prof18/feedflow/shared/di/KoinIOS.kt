package com.prof18.feedflow.shared.di

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.createDatabaseDriver
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.feedflow.shared.domain.browser.BrowserSettingsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncIosWorker
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncWorker
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.presentation.BaseViewModel
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.SettingsViewModel
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
    languageCode: String?,
    regionCode: String?,
    dropboxDataSource: DropboxDataSource,
): KoinApplication = initKoin(
    appEnvironment = appEnvironment,
    modules = listOf(
        module {
            factory { htmlParser }
            single { dropboxDataSource }
            single<FeedFlowStrings> {
                when {
                    languageCode == null -> EnFeedFlowStrings
                    regionCode == null -> feedFlowStrings[languageCode] ?: EnFeedFlowStrings
                    else -> {
                        val locale = "${languageCode}_$regionCode"
                        feedFlowStrings[locale] ?: feedFlowStrings[languageCode] ?: EnFeedFlowStrings
                    }
                }
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

    factory<FeedSyncWorker> {
        FeedSyncIosWorker(
            dispatcherProvider = get(),
            feedSyncMessageQueue = get(),
            dropboxDataSource = get(),
            logger = getWith("FeedSyncIosWorker"),
            feedSyncer = get(),
            appEnvironment = appEnvironment,
            dropboxSettings = get(),
            settingsRepository = get(),
            accountsRepository = get(),
            iCloudSettings = get(),
        )
    }

    viewModel {
        DropboxSyncViewModel(
            logger = getWith("DropboxSyncViewModel"),
            dropboxSettings = get(),
            dropboxDataSource = get(),
            feedSyncRepository = get(),
            dateFormatter = get(),
            feedRetrieverRepository = get(),
            feedSyncMessageQueue = get(),
            accountsRepository = get(),
        )
    }

    factory<CurrentOS> { CurrentOS.Ios }

    viewModel {
        ICloudSyncViewModel(
            iCloudSettings = get(),
            dateFormatter = get(),
            accountsRepository = get(),
            feedSyncMessageQueue = get(),
            feedSyncRepository = get(),
            feedRetrieverRepository = get(),
        )
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
    fun getAccountsViewModel() = getKoin().get<AccountsViewModel>()
    fun getDropboxDataSource() = getKoin().get<DropboxDataSource>()
    fun getDropboxSyncViewModel() = getKoin().get<DropboxSyncViewModel>()
    fun getFeedSyncRepository() = getKoin().get<FeedSyncRepository>()
    fun getICloudSyncViewModel() = getKoin().get<ICloudSyncViewModel>()
}
