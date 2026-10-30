@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedAddState
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.desktop.feedsuggestions.FeedSuggestionsContent
import com.prof18.feedflow.desktop.feedsuggestions.FeedSuggestionsWindow
import com.prof18.feedflow.desktop.importexport.ArticleExportFilterContent
import com.prof18.feedflow.desktop.importexport.ArticleExportFilterDialog
import com.prof18.feedflow.desktop.importexport.ImportExportContent
import com.prof18.feedflow.desktop.testing.FeedSuggestionsE2eIds
import com.prof18.feedflow.desktop.testing.ImportExportE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopSecondaryJourneysTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `mixed OPML result shows invalid feeds and fetch errors together`() {
        val invalid = source("invalid", "Invalid fixture")
        val failed = source("failed", "Fetch error fixture")
        content {
            ImportExport(FeedImportExportState.ImportSuccess(persistentListOf(invalid), persistentListOf(failed)))
        }
        rule.onNodeWithText(EnFeedFlowStrings.wrongLinkReportTitle).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.linkWithErrorReportTitle).assertIsDisplayed()
        rule.onNodeWithText(invalid.url, substring = true).assertIsDisplayed()
        rule.onNodeWithText(failed.url, substring = true).assertIsDisplayed()
    }

    @Test
    fun `import report displays two different URLs even when their source IDs collide`() {
        val first = source("collision", "First fixture").copy(url = "https://first.example.invalid/rss")
        val second = source("collision", "Second fixture").copy(url = "https://second.example.invalid/rss")
        content {
            ImportExport(FeedImportExportState.ImportSuccess(persistentListOf(first, second), persistentListOf()))
        }
        rule.onNodeWithText(first.url, substring = true).assertIsDisplayed()
        rule.onNodeWithText(second.url, substring = true).assertIsDisplayed()
    }

    @Test
    fun `result acknowledgement dispatches once for each OPML and CSV success`() {
        val state = mutableStateOf<FeedImportExportState>(FeedImportExportState.Idle)
        var done = 0
        content { ImportExport(state.value, onDone = { done++ }) }
        val results = listOf(
            FeedImportExportState.ImportSuccess(persistentListOf(), persistentListOf()) to
                ImportExportE2eIds.FEED_IMPORT_SUCCESS,
            FeedImportExportState.ExportSuccess to ImportExportE2eIds.FEED_EXPORT_SUCCESS,
            FeedImportExportState.ArticleImportSuccess to ImportExportE2eIds.ARTICLE_IMPORT_SUCCESS,
            FeedImportExportState.ArticleExportSuccess to ImportExportE2eIds.ARTICLE_EXPORT_SUCCESS,
        )
        results.forEachIndexed { index, result ->
            rule.runOnIdle { state.value = result.first }
            rule.onNodeWithTag(result.second).assertIsDisplayed()
            val action = rule.onNodeWithTag(ImportExportE2eIds.DONE)
                .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
            rule.runOnIdle {
                action()
                action()
                assertEquals(index + 1, done)
            }
        }
    }

    @Test
    fun `invalid OPML offers another file and generic failure offers retry`() {
        val state = mutableStateOf<FeedImportExportState>(FeedImportExportState.InvalidOpml)
        var choose = 0
        var retries = 0
        content { ImportExport(state.value, onChoose = { choose++ }, onRetry = { retries++ }) }
        rule.onNodeWithTag(ImportExportE2eIds.INVALID_OPML).assertIsDisplayed()
        rule.onNodeWithTag(ImportExportE2eIds.CHOOSE_ANOTHER_FILE)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(1, choose)
            assertEquals(0, retries)
            state.value = FeedImportExportState.Error
        }
        rule.onNodeWithTag(ImportExportE2eIds.ERROR).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.retryButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun `OPML and CSV import loading keep their operation labels and hide idle actions`() {
        val state = mutableStateOf<FeedImportExportState>(
            FeedImportExportState.LoadingImport(ImportExportContentType.FeedsOpml),
        )
        content { ImportExport(state.value) }
        rule.onNodeWithText(EnFeedFlowStrings.feedAddInProgressMessage).assertIsDisplayed()
        rule.onNodeWithTag(ImportExportE2eIds.IMPORT_FEEDS).assertDoesNotExist()
        rule.runOnIdle { state.value = FeedImportExportState.LoadingImport(ImportExportContentType.ArticlesCsv) }
        rule.onNodeWithText(EnFeedFlowStrings.articlesImportingMessage).assertIsDisplayed()
        rule.onNodeWithTag(ImportExportE2eIds.EXPORT_ARTICLES).assertDoesNotExist()
    }

    @Test
    fun `captured import click cannot start twice before picker state arrives`() {
        var requests = 0
        content { ImportExport(onImport = { requests++ }) }
        val activate = rule.onNodeWithTag(ImportExportE2eIds.IMPORT_FEEDS)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle {
            activate()
            activate()
            assertEquals(1, requests)
        }
    }

    @Test
    fun `open picker disables all import and export actions and cancellation re-enables them`() {
        val open = mutableStateOf(true)
        var requests = 0
        content { ImportExport(pickerOpen = open.value, onImport = { requests++ }) }
        val tags = listOf(
            ImportExportE2eIds.IMPORT_FEEDS,
            ImportExportE2eIds.EXPORT_FEEDS,
            ImportExportE2eIds.IMPORT_ARTICLES,
            ImportExportE2eIds.EXPORT_ARTICLES,
        )
        tags.forEach { rule.onNodeWithTag(it).assertIsNotEnabled() }
        rule.runOnIdle { open.value = false }
        rule.onNodeWithTag(ImportExportE2eIds.IMPORT_FEEDS)
            .assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertEquals(1, requests)
    }

    @Test
    fun `enlarged RTL report keeps OK readable below a long scrolling report`() {
        val failed = List(size = 30) { source("failed-$it", "A long fixture title ".repeat(n = 20)) }
        compact {
            ImportExport(FeedImportExportState.ImportSuccess(failed.toImmutableList(), persistentListOf()))
        }
        assertReadableAction(EnFeedFlowStrings.doneButton)
    }

    @Test
    fun `enlarged RTL idle content keeps Done visible and closes without an operation`() {
        var closes = 0
        var requests = 0
        compact { ImportExport(onImport = { requests++ }, onClose = { closes++ }) }
        assertReadableAction(EnFeedFlowStrings.actionDone)
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(1, closes)
            assertEquals(0, requests)
        }
    }

    @Test
    fun `enlarged RTL filter keeps Cancel and Confirm readable below scrolling options`() {
        compact {
            ArticleExportFilterContent(ArticleExportFilter.All, {}, {}, {})
        }
        assertReadableAction(EnFeedFlowStrings.cancelButton)
        assertReadableAction(EnFeedFlowStrings.confirmButton)
    }

    @Test
    fun `filter initially focuses its selected option and exposes one selected radio`() {
        content { ArticleExportFilterDialog(ArticleExportFilter.All, {}, {}, {}) }
        rule.onNodeWithText(EnFeedFlowStrings.articlesExportFilterAll).assertIsFocused().assertIsSelected()
    }

    @Test
    fun `cancelled filter cannot export through a captured confirmation`() {
        var exports = 0
        var dismissals = 0
        content {
            ArticleExportFilterDialog(ArticleExportFilter.All, {}, { dismissals++ }, { exports++ })
        }
        val confirm = rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            confirm()
            assertEquals(1, dismissals)
            assertEquals(0, exports)
        }
    }

    @Test
    fun `captured filter confirmation exports the latest selected filter once`() {
        val filter = mutableStateOf(ArticleExportFilter.All)
        val exports = mutableListOf<ArticleExportFilter>()
        content { ArticleExportFilterDialog(filter.value, { filter.value = it }, {}, exports::add) }
        val confirm = rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.onNodeWithText(EnFeedFlowStrings.articlesExportFilterBookmarked)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            confirm()
            confirm()
            assertEquals(listOf(ArticleExportFilter.Bookmarked), exports)
        }
    }

    @Test
    fun `suggestions fallback category exposes selected state with initial focus`() {
        content { Suggestions() }
        rule.onNodeWithTag(FeedSuggestionsE2eIds.category("fixture"))
            .assertIsSelected().assertIsFocused()
    }

    @Test
    fun `suggestion rows show their URLs and identify the add action by feed name`() {
        content { Suggestions() }
        rule.onNodeWithText(suggestedFeed.url).assertIsDisplayed()
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(suggestedFeed.url))
            .assertContentDescriptionEquals("${EnFeedFlowStrings.addFeed}: ${suggestedFeed.name}")
    }

    @Test
    fun `suggestion add states keep a named disabled action and re-enable after explicit reset`() {
        val states = mutableStateOf(persistentMapOf(suggestedFeed.url to FeedAddState.NotAdded))
        var additions = 0
        content { Suggestions(states = states.value, onAdd = { _, _ -> additions++ }) }
        val action = rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(suggestedFeed.url))
        val captured = action.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle { states.value = persistentMapOf(suggestedFeed.url to FeedAddState.Adding) }
        rule.onNodeWithText(EnFeedFlowStrings.addingFeed).assertIsDisplayed()
        action.assertIsNotEnabled()
        rule.runOnIdle {
            captured()
            assertEquals(0, additions)
        }
        rule.runOnIdle { states.value = persistentMapOf(suggestedFeed.url to FeedAddState.Added) }
        rule.onNodeWithText(EnFeedFlowStrings.feedAddedMessageWithoutName).assertIsDisplayed()
        action.assertIsNotEnabled()
        rule.runOnIdle { states.value = persistentMapOf(suggestedFeed.url to FeedAddState.NotAdded) }
        action.assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(1, additions) }
    }

    @Test
    fun `compact enlarged RTL suggestions keep readable Add and Done actions`() {
        var closes = 0
        var additions = 0
        compact { Suggestions(onClose = { closes++ }, onAdd = { _, _ -> additions++ }) }
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(suggestedFeed.url)).performScrollTo()
        assertReadableAction(EnFeedFlowStrings.addFeed)
        assertReadableAction(EnFeedFlowStrings.actionDone)
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(1, closes)
            assertEquals(0, additions)
        }
    }

    @Test
    fun `loading suggestions keeps description and an available close footer`() {
        compact { Suggestions(loading = true) }
        rule.onNodeWithText(EnFeedFlowStrings.feedSuggestionsDescription).assertIsDisplayed()
        assertReadableAction(EnFeedFlowStrings.actionDone)
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(suggestedFeed.url)).assertDoesNotExist()
    }

    @Test
    fun `empty suggestions catalog explains the state and focuses keyboard dismissal`() {
        var closes = 0
        var additions = 0
        compact {
            Suggestions(categories = persistentListOf(), onClose = { closes++ }, onAdd = { _, _ -> additions++ })
        }
        assertReadableAction("No feed suggestions are available.")
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle {
            assertEquals(1, closes)
            assertEquals(0, additions)
        }
    }

    @Test
    fun `empty selected category keeps category navigation and explains the missing rows`() {
        content { Suggestions(categories = persistentListOf(suggestedCategory.copy(feeds = emptyList()))) }
        rule.onNodeWithTag(FeedSuggestionsE2eIds.category(suggestedCategory.id))
            .assertIsSelected().assertIsFocused()
        rule.onNodeWithText("No feed suggestions in this category.").assertIsDisplayed()
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(suggestedFeed.url)).assertDoesNotExist()
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `loading an empty catalog does not announce an empty result before loading ends`() {
        val loading = mutableStateOf(true)
        content { Suggestions(categories = persistentListOf(), loading = loading.value) }
        rule.onNodeWithText("No feed suggestions are available.").assertDoesNotExist()
        rule.runOnIdle { loading.value = false }
        rule.onNodeWithText("No feed suggestions are available.").assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).assertIsFocused()
    }

    @Test
    fun `suggestion category selection sends the displayed category name when adding`() {
        val selected = mutableStateOf<String?>(null)
        val additions = mutableListOf<Pair<SuggestedFeed, String>>()
        val second = suggestedFeed.copy(name = "Other fixture", url = "https://other.example.invalid/rss")
        val categories = persistentListOf(
            suggestedCategory,
            SuggestedFeedCategory("other", "Other category", "", listOf(second)),
        )
        content {
            FeedSuggestionsContent(
                categories = categories,
                selectedCategoryId = selected.value,
                feedStates = persistentMapOf(),
                isLoading = false,
                onCategorySelected = { selected.value = it },
                onAddFeed = { feed, category -> additions += feed to category },
                onCloseRequest = {},
            )
        }
        rule.onNodeWithTag(FeedSuggestionsE2eIds.category("other"))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag(FeedSuggestionsE2eIds.addButton(second.url))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(listOf(second to "Other category"), additions) }
    }

    @Test
    fun `initial suggestions selection scrolls a distant category into focus`() {
        val categories = List(size = 12) { index ->
            SuggestedFeedCategory("fixture-$index", "Fixture category $index", "", listOf(suggestedFeed))
        }.toImmutableList()
        content {
            FeedSuggestionsContent(
                categories = categories,
                selectedCategoryId = "fixture-11",
                feedStates = persistentMapOf(),
                isLoading = false,
                onCategorySelected = {},
                onAddFeed = { _, _ -> },
                onCloseRequest = {},
            )
        }
        rule.onNodeWithTag(FeedSuggestionsE2eIds.category("fixture-11"))
            .assertIsDisplayed().assertIsSelected().assertIsFocused()
    }

    @Test
    fun `native Escape closes suggestions without adding a feed`() {
        val visible = mutableStateOf(true)
        var additions = 0
        content {
            if (visible.value) {
                FeedSuggestionsWindow(onCloseRequest = { visible.value = false }) { modifier ->
                    Suggestions(modifier = modifier, onAdd = { _, _ -> additions++ })
                }
            }
        }
        rule.waitUntilExactlyOneExists(
            androidx.compose.ui.test.hasTestTag(FeedSuggestionsE2eIds.SCREEN),
            timeoutMillis = 5_000,
        )
        SwingUtilities.invokeAndWait {
            val window = Window.getWindows().filterIsInstance<Dialog>().single {
                it.isVisible && it.title == EnFeedFlowStrings.feedSuggestionsTitle
            }
            val input = descendants(window).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val event = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(event) }
        }
        rule.waitUntil(timeoutMillis = 5_000) { !visible.value }
        rule.runOnIdle { assertEquals(0, additions) }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private val suggestedFeed = SuggestedFeed(
        name = "Fixture source",
        url = "https://fixture.example.invalid/rss",
        description = "A deterministic suggestion fixture with no remote image",
    )
    private val suggestedCategory = SuggestedFeedCategory("fixture", "Fixture category", "", listOf(suggestedFeed))

    @Composable
    private fun Suggestions(
        modifier: Modifier = Modifier,
        categories: ImmutableList<SuggestedFeedCategory> = persistentListOf(suggestedCategory),
        states: ImmutableMap<String, FeedAddState> = persistentMapOf(),
        loading: Boolean = false,
        onAdd: (SuggestedFeed, String) -> Unit = { _, _ -> },
        onClose: () -> Unit = {},
    ) {
        FeedSuggestionsContent(
            categories = categories,
            selectedCategoryId = null,
            feedStates = states,
            isLoading = loading,
            onCategorySelected = {},
            onAddFeed = onAdd,
            onCloseRequest = onClose,
            modifier = modifier,
        )
    }

    private fun source(id: String, title: String) = ParsedFeedSource(
        id = id,
        url = "https://$id.example.invalid/rss",
        title = title,
        category = null,
        logoUrl = null,
        websiteUrl = null,
    )

    private fun assertReadableAction(text: String) {
        rule.onNodeWithText(text).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(!layouts.single().hasVisualOverflow, "$text must be fully readable")
    }

    private fun compact(body: @Composable () -> Unit) {
        content(fontScale = 2f, direction = LayoutDirection.Rtl) {
            Box(Modifier.size(width = 360.dp, height = 500.dp)) { body() }
        }
    }

    private fun content(
        fontScale: Float = 1f,
        direction: LayoutDirection = LayoutDirection.Ltr,
        body: @Composable () -> Unit,
    ) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(density.density, fontScale),
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }

    @Composable
    private fun ImportExport(
        state: FeedImportExportState = FeedImportExportState.Idle,
        onDone: () -> Unit = {},
        onChoose: () -> Unit = {},
        onRetry: () -> Unit = {},
        onImport: () -> Unit = {},
        onClose: () -> Unit = {},
        pickerOpen: Boolean = false,
    ) {
        ImportExportContent(
            feedImportExportState = state,
            onRetryClick = onRetry,
            onChooseAnotherFileClick = onChoose,
            onDoneClick = onDone,
            onImportClick = onImport,
            onExportClick = {},
            onImportArticlesClick = {},
            onExportArticlesClick = {},
            isFilePickerOpen = pickerOpen,
            onCloseClick = onClose,
        )
    }
}
