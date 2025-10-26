package com.prof18.feedflow.android

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.prof18.feedflow.android.accounts.AccountsScreen
import com.prof18.feedflow.android.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.android.addfeed.AddFeedScreen
import com.prof18.feedflow.android.base.BaseThemeActivity
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
import com.prof18.feedflow.android.settings.blocked.BlockedWordsScreen
import com.prof18.feedflow.android.settings.importexport.ImportExportScreen
import com.prof18.feedflow.android.settings.notifications.NotificationsSettingsScreen
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.shouldOpenInBrowser
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.shared.presentation.DeeplinkFeedViewModel
import com.prof18.feedflow.shared.presentation.EditFeedViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.presentation.ReviewViewModel
import com.prof18.feedflow.shared.presentation.model.DeeplinkFeedState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : BaseThemeActivity() {

    private val messageQueue by inject<FeedSyncMessageQueue>()
    private val reviewViewModel by viewModel<ReviewViewModel>()
    private val homeViewModel by viewModel<HomeViewModel>()
    private val browserManager by inject<BrowserManager>()

    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntent = intent

        if (BuildConfig.FLAVOR == "googlePlay") {
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    reviewViewModel.canShowReviewDialog.collect { showReview ->
                        if (showReview) {
                            PlayReviewManager.triggerReviewFlow(
                                this@MainActivity,
                                onReviewDone = {
                                    reviewViewModel.onReviewShown()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }

    @Composable
    override fun Content() {
        val readerModeViewModel: ReaderModeViewModel = koinViewModel()
        val deeplinkViewModel: DeeplinkFeedViewModel = koinViewModel()
        val snackbarHostState = remember { SnackbarHostState() }

        val navController = rememberNavController()
        val flowStrings = LocalFeedFlowStrings.current

        val deeplinkState by deeplinkViewModel.deeplinkFeedState.collectAsStateWithLifecycle()

        LaunchedEffect(currentIntent) {
            currentIntent?.let { intent ->
                if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
                    val uri = intent.data
                    if (uri?.scheme == "feedflow" && uri.host == "feed") {
                        val feedId = uri.pathSegments.firstOrNull()
                        if (feedId != null) {
                            readerModeViewModel.setLoading()
                            deeplinkViewModel.getReaderModeUrl(FeedItemId(feedId))
                        }
                    }
                }
            }
        }

        LaunchedEffect(deeplinkState) {
            handleDeepLinkState(
                state = deeplinkState,
                deeplinkViewModel = deeplinkViewModel,
                readerModeViewModel = readerModeViewModel,
                navController = navController,
            )
        }

        LaunchedEffect(Unit) {
            messageQueue.messageQueue.collect { message ->
                if (message is SyncResult.Error) {
                    snackbarHostState.showSnackbar(
                        message = flowStrings.errorAccountSync(message.errorCode.code),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            FeedFlowNavigation(
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

    @Composable
    private fun FeedFlowNavigation(
        navController: NavHostController,
        readerModeViewModel: ReaderModeViewModel,
    ) {
        NavHost(
            navController = navController,
            startDestination = Home(),
            enterTransition = { fadeIn() + slideIntoContainer(SlideDirection.Start) },
            exitTransition = { fadeOut() + slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f),
                )
            },
        ) {
            composable<Home>(
                deepLinks = listOf(
                    navDeepLink {
                        action = Intent.ACTION_VIEW
                        uriPattern = "feedflow://feedsourcefilter/{feedSourceId}"
                    },
                ),
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<Home>()

                val savedStateHandle = backStackEntry.savedStateHandle

                LaunchedEffect(Unit) {
                    val isConsumed = savedStateHandle.get<Boolean>("deepLinkConsumed") ?: false
                    if (!isConsumed) {
                        val feedId: String? = route.feedSourceId
                        if (!feedId.isNullOrEmpty()) {
                            homeViewModel.updateFeedSourceFilter(feedId)
                            savedStateHandle["deepLinkConsumed"] = true
                        }
                    }
                }

                HomeScreen(
                    homeViewModel = homeViewModel,
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
                    navigateToNotifications = {
                        navController.navigate(Notifications)
                    },
                    navigateToBlockedWords = {
                        navController.navigate(BlockedWords)
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
                    onDoneClick = {
                        homeViewModel.getNewFeeds()
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
                    navigateToEditFeed = { feedSource ->
                        navController.navigate(feedSource.toEditFeed())
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

            composable<Notifications> {
                NotificationsSettingsScreen(
                    navigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<BlockedWords> {
                BlockedWordsScreen(
                    navigateBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }

    private fun handleDeepLinkState(
        state: DeeplinkFeedState,
        deeplinkViewModel: DeeplinkFeedViewModel,
        readerModeViewModel: ReaderModeViewModel,
        navController: NavHostController,
    ) {
        if (state is DeeplinkFeedState.Success) {
            val feedUrlInfo = state.data
            deeplinkViewModel.markAsRead(FeedItemId(feedUrlInfo.id))
            handleLinkOpeningPreference(feedUrlInfo, readerModeViewModel, navController)
        }
    }

    private fun handleLinkOpeningPreference(
        feedUrlInfo: FeedItemUrlInfo,
        readerModeViewModel: ReaderModeViewModel,
        navController: NavHostController,
    ) {
        when (feedUrlInfo.linkOpeningPreference) {
            LinkOpeningPreference.READER_MODE -> {
                navigateToReaderModeIfNeeded(readerModeViewModel, navController, feedUrlInfo)
            }
            LinkOpeningPreference.INTERNAL_BROWSER -> {
                browserManager.openWithInAppBrowser(feedUrlInfo.url, this@MainActivity)
            }
            LinkOpeningPreference.PREFERRED_BROWSER -> {
                browserManager.openUrlWithFavoriteBrowser(feedUrlInfo.url, this@MainActivity)
            }
            LinkOpeningPreference.DEFAULT -> {
                if (browserManager.openReaderMode() && !feedUrlInfo.shouldOpenInBrowser()) {
                    navigateToReaderModeIfNeeded(readerModeViewModel, navController, feedUrlInfo)
                } else {
                    browserManager.openUrlWithFavoriteBrowser(feedUrlInfo.url, this@MainActivity)
                }
            }
        }
    }

    private fun navigateToReaderModeIfNeeded(
        readerModeViewModel: ReaderModeViewModel,
        navController: NavHostController,
        feedUrlInfo: FeedItemUrlInfo,
    ) {
        readerModeViewModel.getReaderModeHtml(feedUrlInfo)
        if (navController.currentDestination?.hasRoute(ReaderMode::class) == false) {
            navController.navigate(ReaderMode)
        }
    }
}
