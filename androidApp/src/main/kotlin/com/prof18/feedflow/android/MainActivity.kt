package com.prof18.feedflow.android

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.prof18.feedflow.android.accounts.AccountsScreen
import com.prof18.feedflow.android.accounts.bazqux.BazquxSyncScreen
import com.prof18.feedflow.android.accounts.feedbin.FeedbinSyncScreen
import com.prof18.feedflow.android.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.android.accounts.miniflux.MinifluxSyncScreen
import com.prof18.feedflow.android.addfeed.AddFeedScreen
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.android.editfeed.EditScreen
import com.prof18.feedflow.android.editfeed.toEditFeed
import com.prof18.feedflow.android.editfeed.toFeedSource
import com.prof18.feedflow.android.feedsourcelist.FeedSourceListScreen
import com.prof18.feedflow.android.feedsuggestions.FeedSuggestionsScreen
import com.prof18.feedflow.android.home.HomeScreen
import com.prof18.feedflow.android.readermode.ReaderModeScreen
import com.prof18.feedflow.android.search.SearchScreen
import com.prof18.feedflow.android.settings.SettingsScreen
import com.prof18.feedflow.android.settings.about.AboutAndSupportScreen
import com.prof18.feedflow.android.settings.about.subpages.AboutScreen
import com.prof18.feedflow.android.settings.about.subpages.LicensesScreen
import com.prof18.feedflow.android.settings.extras.ExtrasScreen
import com.prof18.feedflow.android.settings.feedlist.FeedListSettingsScreen
import com.prof18.feedflow.android.settings.feedsandaccounts.FeedsAndAccountsScreen
import com.prof18.feedflow.android.settings.feedsandaccounts.subpages.BlockedWordsScreen
import com.prof18.feedflow.android.settings.feedsandaccounts.subpages.ImportExportScreen
import com.prof18.feedflow.android.settings.feedsandaccounts.subpages.NotificationsSettingsScreen
import com.prof18.feedflow.android.settings.readingbehavior.ReadingBehaviorScreen
import com.prof18.feedflow.android.settings.syncstorage.SyncAndStorageScreen
import com.prof18.feedflow.android.settings.widget.WidgetSettingsScreen
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
import com.prof18.feedflow.shared.presentation.ThemeViewModel
import com.prof18.feedflow.shared.presentation.model.DeeplinkFeedState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
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
        homeViewModel.onAppLaunch()

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

        val backStack = rememberNavBackStack(Home())
        val flowStrings = LocalFeedFlowStrings.current

        val deeplinkState by deeplinkViewModel.deeplinkFeedState.collectAsStateWithLifecycle()

        LaunchedEffect(currentIntent) {
            currentIntent?.let { intent ->
                if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
                    val uri = intent.data
                    if (uri?.scheme == "feedflow") {
                        when (uri.host) {
                            "feed" -> {
                                val feedId = uri.pathSegments.firstOrNull()
                                if (feedId != null) {
                                    readerModeViewModel.setLoading()
                                    deeplinkViewModel.getReaderModeUrl(FeedItemId(feedId))
                                }
                            }
                            "feedsourcefilter" -> {
                                val feedSourceId = uri.pathSegments.firstOrNull()
                                if (feedSourceId != null) {
                                    homeViewModel.updateFeedSourceFilter(feedSourceId)
                                }
                            }
                            "category" -> {
                                val categoryId = uri.pathSegments.firstOrNull()
                                if (categoryId != null) {
                                    homeViewModel.updateCategoryFilter(categoryId)
                                }
                            }
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
                backStack = backStack,
            )
        }

        LaunchedEffect(Unit) {
            messageQueue.messageQueue.collect { message ->
                if (message is SyncResult.GoogleDriveNeedReAuth) {
                    snackbarHostState.showSnackbar(
                        message = flowStrings.googleDriveAuthRetry,
                    )
                } else if (message is SyncResult.Error) {
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
                backStack = backStack,
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

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun FeedFlowNavigation(
        backStack: NavBackStack<NavKey>,
        readerModeViewModel: ReaderModeViewModel,
    ) {
        val reduceMotionEnabled = LocalReduceMotion.current
        val navigateBack: () -> Unit = { popBackStackOrFinish(backStack) }

        NavDisplay(
            backStack = backStack,
            onBack = navigateBack,
            transitionSpec = {
                if (reduceMotionEnabled) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)) togetherWith
                        (fadeOut() + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start))
                }
            },
            popTransitionSpec = {
                if (reduceMotionEnabled) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val rightEdgeOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
                    EnterTransition.None togetherWith scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = rightEdgeOrigin,
                    )
                }
            },
            predictivePopTransitionSpec = { swipeEdge: Int ->
                if (reduceMotionEnabled) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val origin = when (swipeEdge) {
                        NavigationEvent.EDGE_LEFT -> TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
                        NavigationEvent.EDGE_RIGHT -> TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
                        else -> TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
                    }
                    EnterTransition.None togetherWith scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = origin,
                    )
                }
            },
            entryProvider = entryProvider {
                entry<FeedSuggestions> {
                    FeedSuggestionsScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<Home> { route ->
                    LaunchedEffect(Unit) {
                        val feedId: String? = route.feedSourceId
                        if (!feedId.isNullOrEmpty()) {
                            homeViewModel.updateFeedSourceFilter(feedId)
                        }

                        val categoryId: String? = route.categoryId
                        if (!categoryId.isNullOrEmpty()) {
                            homeViewModel.updateCategoryFilter(categoryId)
                        }
                    }

                    HomeScreen(
                        homeViewModel = homeViewModel,
                        onSettingsButtonClicked = { backStack.add(Settings) },
                        onAddFeedClick = { backStack.add(AddFeed) },
                        onImportExportClick = { backStack.add(ImportExport) },
                        navigateToReaderMode = { url ->
                            readerModeViewModel.getReaderModeHtml(url)
                            backStack.add(ReaderMode)
                        },
                        onSearchClick = { backStack.add(Search) },
                        onAccountsClick = { backStack.add(Accounts) },
                        onEditFeedClick = { feedSource ->
                            backStack.add(feedSource.toEditFeed())
                        },
                        onFeedSuggestionsClick = { backStack.add(FeedSuggestions) },
                    )
                }

                entry<Settings> {
                    SettingsScreen(
                        navigateBack = navigateBack,
                        navigateToFeedsAndAccounts = { backStack.add(FeedsAndAccounts) },
                        navigateToFeedListSettings = { backStack.add(FeedListSettings) },
                        navigateToReadingBehavior = { backStack.add(ReadingBehavior) },
                        navigateToSyncAndStorage = { backStack.add(SyncAndStorage) },
                        navigateToWidgetSettings = { backStack.add(WidgetSettings) },
                        navigateToExtras = { backStack.add(Extras) },
                        navigateToAboutAndSupport = { backStack.add(AboutAndSupport) },
                    )
                }

                entry<FeedsAndAccounts> {
                    FeedsAndAccountsScreen(
                        navigateBack = navigateBack,
                        onFeedListClick = { backStack.add(FeedList) },
                        onAddFeedClick = { backStack.add(AddFeed) },
                        navigateToImportExport = { backStack.add(ImportExport) },
                        navigateToAccounts = { backStack.add(Accounts) },
                        navigateToNotifications = { backStack.add(Notifications) },
                        navigateToBlockedWords = { backStack.add(BlockedWords) },
                    )
                }

                entry<FeedListSettings> {
                    FeedListSettingsScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<ReadingBehavior> {
                    ReadingBehaviorScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<SyncAndStorage> {
                    SyncAndStorageScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<Extras> {
                    ExtrasScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<WidgetSettings> {
                    WidgetSettingsScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<AboutAndSupport> {
                    AboutAndSupportScreen(
                        navigateBack = navigateBack,
                        onAboutClick = { backStack.add(About) },
                    )
                }

                entry<AddFeed> {
                    AddFeedScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<FeedList> {
                    FeedSourceListScreen(
                        onAddFeedClick = { backStack.add(AddFeed) },
                        navigateBack = navigateBack,
                        onEditFeedClick = { feedSource ->
                            backStack.add(feedSource.toEditFeed())
                        },
                    )
                }

                entry<About> {
                    AboutScreen(
                        onBackClick = navigateBack,
                        navigateToLibrariesScreen = { backStack.add(Licenses) },
                    )
                }

                entry<Licenses> {
                    LicensesScreen(
                        onBackClick = navigateBack,
                    )
                }

                entry<ImportExport> {
                    ImportExportScreen(
                        navigateBack = navigateBack,
                        refreshFeeds = {
                            homeViewModel.getNewFeeds()
                        },
                    )
                }

                entry<ReaderMode> {
                    val readerModeState by readerModeViewModel.readerModeState.collectAsStateWithLifecycle()
                    val fontSizeState by readerModeViewModel.readerFontSizeState.collectAsStateWithLifecycle()
                    val canNavigatePrevious by readerModeViewModel.canNavigateToPreviousState
                        .collectAsStateWithLifecycle()
                    val canNavigateNext by readerModeViewModel.canNavigateToNextState
                        .collectAsStateWithLifecycle()

                    val themeViewModel = koinViewModel<ThemeViewModel>()
                    val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()

                    ReaderModeScreen(
                        readerModeState = readerModeState,
                        fontSize = fontSizeState,
                        themeMode = themeState,
                        navigateBack = navigateBack,
                        onUpdateFontSize = { newFontSize ->
                            readerModeViewModel.updateFontSize(newFontSize)
                        },
                        onBookmarkClick = { feedItemId: FeedItemId, isBookmarked: Boolean ->
                            readerModeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                        },
                        canNavigatePrevious = canNavigatePrevious,
                        canNavigateNext = canNavigateNext,
                        onNavigateToPrevious = {
                            readerModeViewModel.navigateToPreviousArticle()
                        },
                        onNavigateToNext = {
                            readerModeViewModel.navigateToNextArticle()
                        },
                    )
                }

                entry<Search> {
                    SearchScreen(
                        navigateBack = navigateBack,
                        navigateToReaderMode = { urlInfo ->
                            readerModeViewModel.getReaderModeHtml(urlInfo)
                            backStack.add(ReaderMode)
                        },
                        navigateToEditFeed = { feedSource ->
                            backStack.add(feedSource.toEditFeed())
                        },
                    )
                }

                entry<Accounts> {
                    AccountsScreen(
                        navigateBack = navigateBack,
                        navigateToFreshRssSync = { backStack.add(FreshRssSync) },
                        navigateToMinifluxSync = { backStack.add(MinifluxSync) },
                        navigateToBazquxSync = { backStack.add(BazquxSync) },
                        navigateToFeedbinSync = { backStack.add(FeedbinSync) },
                    )
                }

                entry<EditFeed> { route ->
                    val feedSource: FeedSource = route.toFeedSource()
                    val viewModel = koinViewModel<EditFeedViewModel>()

                    LaunchedEffect(feedSource) {
                        viewModel.loadFeedToEdit(feedSource)
                    }

                    EditScreen(
                        viewModel = viewModel,
                        navigateBack = navigateBack,
                    )
                }

                entry<FreshRssSync> {
                    FreshRssSyncScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<MinifluxSync> {
                    MinifluxSyncScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<BazquxSync> {
                    BazquxSyncScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<FeedbinSync> {
                    FeedbinSyncScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<Notifications> {
                    NotificationsSettingsScreen(
                        navigateBack = navigateBack,
                    )
                }

                entry<BlockedWords> {
                    BlockedWordsScreen(
                        navigateBack = navigateBack,
                    )
                }
            },
        )
    }

    private fun popBackStackOrFinish(backStack: NavBackStack<NavKey>) {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            finish()
        }
    }

    private fun handleDeepLinkState(
        state: DeeplinkFeedState,
        deeplinkViewModel: DeeplinkFeedViewModel,
        readerModeViewModel: ReaderModeViewModel,
        backStack: NavBackStack<NavKey>,
    ) {
        if (state is DeeplinkFeedState.Success) {
            val feedUrlInfo = state.data
            deeplinkViewModel.markAsRead(FeedItemId(feedUrlInfo.id))
            handleLinkOpeningPreference(feedUrlInfo, readerModeViewModel, backStack)
        }
    }

    private fun handleLinkOpeningPreference(
        feedUrlInfo: FeedItemUrlInfo,
        readerModeViewModel: ReaderModeViewModel,
        backStack: NavBackStack<NavKey>,
    ) {
        when (feedUrlInfo.linkOpeningPreference) {
            LinkOpeningPreference.READER_MODE -> {
                navigateToReaderModeIfNeeded(readerModeViewModel, backStack, feedUrlInfo)
            }
            LinkOpeningPreference.INTERNAL_BROWSER -> {
                browserManager.openWithInAppBrowser(feedUrlInfo.url, this@MainActivity)
            }
            LinkOpeningPreference.PREFERRED_BROWSER -> {
                browserManager.openUrlWithFavoriteBrowser(feedUrlInfo.url, this@MainActivity)
            }
            LinkOpeningPreference.DEFAULT -> {
                if (browserManager.openReaderMode() && !feedUrlInfo.shouldOpenInBrowser()) {
                    navigateToReaderModeIfNeeded(readerModeViewModel, backStack, feedUrlInfo)
                } else {
                    browserManager.openUrlWithFavoriteBrowser(feedUrlInfo.url, this@MainActivity)
                }
            }
        }
    }

    private fun navigateToReaderModeIfNeeded(
        readerModeViewModel: ReaderModeViewModel,
        backStack: NavBackStack<NavKey>,
        feedUrlInfo: FeedItemUrlInfo,
    ) {
        readerModeViewModel.getReaderModeHtml(feedUrlInfo)
        if (!backStack.contains(ReaderMode)) {
            backStack.add(ReaderMode)
        }
    }
}
