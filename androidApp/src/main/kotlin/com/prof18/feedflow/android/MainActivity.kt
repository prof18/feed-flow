package com.prof18.feedflow.android

import FeedFlowTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.prof18.feedflow.android.accounts.AccountsScreen
import com.prof18.feedflow.android.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.android.addfeed.AddFeedScreen
import com.prof18.feedflow.android.editfeed.EditScreen
import com.prof18.feedflow.android.editfeed.toEditFeed
import com.prof18.feedflow.android.editfeed.toFeedSource
import com.prof18.feedflow.android.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.android.home.HomeScreen
import com.prof18.feedflow.android.readermode.ReaderModeScreen
import com.prof18.feedflow.android.search.SearchScreen
import com.prof18.feedflow.android.settings.SettingsScreen
import com.prof18.feedflow.android.settings.about.AboutScreen
import com.prof18.feedflow.android.settings.about.LicensesScreen
import com.prof18.feedflow.android.settings.importexport.ImportExportScreen
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private val messageQueue by inject<FeedSyncMessageQueue>()
    private val homeViewModel by viewModel<HomeViewModel>()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val koin = getKoin()
            setSingletonImageLoaderFactory { koin.get<ImageLoader>() }
            val readerModeViewModel: ReaderModeViewModel = koinViewModel()

            val windowSize = calculateWindowSizeClass(this@MainActivity)
            val snackbarHostState = remember { SnackbarHostState() }

            FeedFlowTheme {
                val navController = rememberNavController()
                val lyricist = rememberFeedFlowStrings()
                ProvideFeedFlowStrings(lyricist) {
                    val errorMessage = LocalFeedFlowStrings.current.errorAccountSync
                    LaunchedEffect(Unit) {
                        messageQueue.messageQueue.collect { message ->
                            if (message is SyncResult.Error) {
                                snackbarHostState.showSnackbar(
                                    message = errorMessage,
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        FeedFlowNavigation(
                            windowSizeClass = windowSize,
                            navController = navController,
                            readerModeViewModel = readerModeViewModel,
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            SnackbarHost(snackbarHostState)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadFeeds()
    }

    @Composable
    private fun FeedFlowNavigation(
        windowSizeClass: WindowSizeClass,
        navController: NavHostController,
        readerModeViewModel: ReaderModeViewModel,
    ) {
        NavHost(
            navController = navController,
            startDestination = Home,
            enterTransition = { fadeIn() + slideIntoContainer(SlideDirection.Start) },
            exitTransition = { fadeOut() + slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { fadeIn() + slideIntoContainer(SlideDirection.End) },
            popExitTransition = { fadeOut() + slideOutOfContainer(SlideDirection.End) },
        ) {
            composable<Home> {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    windowSizeClass = windowSizeClass,
                    onSettingsButtonClicked = {
                        navController.navigate(Settings)
                    },
                    onAddFeedClick = {
                        navController.navigate(AddFeed)
                    },
                    onImportExportClick = {
                        navController.navigate(ImportExport)
                    },
                    navigateToReaderMode = { url ->
                        readerModeViewModel.getReaderModeHtml(url)
                        navController.navigate(ReaderMode)
                    },
                    onSearchClick = {
                        navController.navigate(Search)
                    },
                    onAccountsClick = {
                        navController.navigate(Accounts)
                    },
                    onEditFeedClick = { feedSource ->
                        navController.navigate(feedSource.toEditFeed())
                    },
                )
            }

            composable<Settings> {
                SettingsScreen(
                    onFeedListClick = {
                        navController.navigate(FeedList)
                    },
                    onAddFeedClick = {
                        navController.navigate(AddFeed)
                    },
                    navigateBack = {
                        navController.popBackStack()
                    },
                    onAboutClick = {
                        navController.navigate(About)
                    },
                    navigateToImportExport = {
                        navController.navigate(ImportExport)
                    },
                    navigateToAccounts = {
                        navController.navigate(Accounts)
                    },
                )
            }

            composable<AddFeed> {
                AddFeedScreen(
                    navigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<FeedList> {
                FeedSourceListScreen(
                    onAddFeedClick = {
                        navController.navigate(AddFeed)
                    },
                    navigateBack = {
                        navController.popBackStack()
                    },
                    onEditFeedClick = { feedSource ->
                        navController.navigate(feedSource.toEditFeed())
                    },
                )
            }

            composable<About> {
                AboutScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToLibrariesScreen = {
                        navController.navigate(Licenses)
                    },
                )
            }

            composable<Licenses> {
                LicensesScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }

            composable<ImportExport> {
                ImportExportScreen(
                    navigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<ReaderMode> {
                val readerModeState by readerModeViewModel.readerModeState.collectAsStateWithLifecycle()
                val fontSizeState by readerModeViewModel.readerFontSizeState.collectAsStateWithLifecycle()

                ReaderModeScreen(
                    readerModeState = readerModeState,
                    fontSize = fontSizeState,
                    navigateBack = {
                        navController.popBackStack()
                    },
                    onUpdateFontSize = { newFontSize ->
                        readerModeViewModel.updateFontSize(newFontSize)
                    },
                    onBookmarkClick = { feedItemId: FeedItemId, isBookmarked: Boolean ->
                        readerModeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                    },
                )
            }

            composable<Search> {
                SearchScreen(
                    navigateBack = {
                        navController.popBackStack()
                    },
                    navigateToReaderMode = { urlInfo ->
                        readerModeViewModel.getReaderModeHtml(urlInfo)
                        navController.navigate(ReaderMode)
                    },
                )
            }

            composable<Accounts> {
                AccountsScreen(
                    navigateBack = {
                        navController.popBackStack()
                    },
                    navigateToFreshRssSync = {
                        navController.navigate(FreshRssSync)
                    },
                )
            }

            composable<EditFeed> { backstackEntry ->
                val feedSource: FeedSource = backstackEntry.toRoute<EditFeed>().toFeedSource()
                val viewModel = koinViewModel<EditFeedViewModel>()

                LaunchedEffect(feedSource) {
                    viewModel.loadFeedToEdit(feedSource)
                }

                EditScreen(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<FreshRssSync> {
                FreshRssSyncScreen(
                    navigateBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
