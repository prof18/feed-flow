package com.prof18.feedflow.desktop.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.rememberScrollbarState
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.desktop.settings.SettingsBlock
import com.prof18.feedflow.desktop.settings.SettingsSection
import com.prof18.feedflow.desktop.testing.ImportExportE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun ImportExportContent(
    feedImportExportState: FeedImportExportState,
    onRetryClick: () -> Unit,
    onChooseAnotherFileClick: () -> Unit,
    onDoneClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportArticlesClick: () -> Unit,
    onExportArticlesClick: (ArticleExportFilter) -> Unit,
    modifier: Modifier = Modifier,
    isFilePickerOpen: Boolean = false,
    onCloseClick: () -> Unit = {},
) {
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(ArticleExportFilter.All) }
    var requestPending by rememberSaveable { mutableStateOf(false) }
    var lastAction by rememberSaveable { mutableStateOf<ImportExportAction?>(null) }
    var focusRevision by rememberSaveable { mutableStateOf(0) }
    val importFeeds by rememberUpdatedState(onImportClick)
    val exportFeeds by rememberUpdatedState(onExportClick)
    val importArticles by rememberUpdatedState(onImportArticlesClick)
    val exportArticles by rememberUpdatedState(onExportArticlesClick)
    val close by rememberUpdatedState(onCloseClick)
    LaunchedEffect(isFilePickerOpen, showFilterDialog, feedImportExportState) {
        if (!isFilePickerOpen && !showFilterDialog) {
            requestPending = false
            if (feedImportExportState == FeedImportExportState.Idle) focusRevision++
        }
    }
    val dispatch: (ImportExportAction) -> Unit = { action ->
        if (!requestPending && !isFilePickerOpen && !showFilterDialog) {
            lastAction = action
            if (action == ImportExportAction.ExportArticles) {
                showFilterDialog = true
            } else {
                requestPending = true
                dispatchFileAction(action, importFeeds, exportFeeds, importArticles)
            }
        }
    }
    if (showFilterDialog) {
        ArticleExportFilterDialog(
            filter = filter,
            onFilterChange = { filter = it },
            onDismiss = { showFilterDialog = false },
            onExport = { chosen ->
                showFilterDialog = false
                requestPending = true
                exportArticles(chosen)
            },
        )
    }
    if (feedImportExportState == FeedImportExportState.Idle) {
        ImportExportIdleContent(
            enabled = !isFilePickerOpen && !requestPending,
            lastAction = lastAction,
            focusRevision = focusRevision,
            onAction = dispatch,
            onClose = {
                if (!isFilePickerOpen && !requestPending && !showFilterDialog) {
                    requestPending = true
                    close()
                }
            },
            modifier = modifier,
        )
    } else {
        ImportExportResultContent(
            state = feedImportExportState,
            onRetryClick = onRetryClick,
            onChooseAnotherFileClick = onChooseAnotherFileClick,
            onDoneClick = onDoneClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun ImportExportResultContent(
    state: FeedImportExportState,
    onRetryClick: () -> Unit,
    onChooseAnotherFileClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    when (state) {
        FeedImportExportState.Idle -> Unit
        is FeedImportExportState.LoadingImport, is FeedImportExportState.LoadingExport -> Column(
            modifier = modifier.fillMaxSize().padding(DesktopDesignTokens.spacePanel),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(
                text = when (state) {
                    is FeedImportExportState.LoadingImport -> {
                        if (state.contentType == ImportExportContentType.FeedsOpml) {
                            strings.feedAddInProgressMessage
                        } else {
                            strings.articlesImportingMessage
                        }
                    }
                    else -> strings.exportStartedMessage
                },
                modifier = Modifier.padding(top = DesktopDesignTokens.spaceSection),
                textAlign = TextAlign.Center,
            )
        }
        FeedImportExportState.Error -> ResultView(
            message = strings.genericErrorMessage,
            action = strings.retryButton,
            onAction = onRetryClick,
            modifier = modifier,
            messageTestTag = ImportExportE2eIds.ERROR,
        )
        FeedImportExportState.InvalidOpml -> ResultView(
            message = strings.invalidOpmlImportTitle,
            action = strings.chooseAnotherFileButton,
            onAction = onChooseAnotherFileClick,
            modifier = modifier,
            messageTestTag = ImportExportE2eIds.INVALID_OPML,
            actionTestTag = ImportExportE2eIds.CHOOSE_ANOTHER_FILE,
        )
        FeedImportExportState.ExportSuccess -> ResultView(
            message = strings.feedsExportDoneMessage,
            action = strings.doneButton,
            onAction = onDoneClick,
            modifier = modifier,
            messageTestTag = ImportExportE2eIds.FEED_EXPORT_SUCCESS,
            actionTestTag = ImportExportE2eIds.DONE,
        )
        FeedImportExportState.ArticleExportSuccess -> ResultView(
            message = strings.articlesExportDoneMessage,
            action = strings.doneButton,
            onAction = onDoneClick,
            modifier = modifier,
            messageTestTag = ImportExportE2eIds.ARTICLE_EXPORT_SUCCESS,
            actionTestTag = ImportExportE2eIds.DONE,
        )
        FeedImportExportState.ArticleImportSuccess -> ResultView(
            message = strings.articlesImportDoneMessage,
            action = strings.doneButton,
            onAction = onDoneClick,
            modifier = modifier,
            messageTestTag = ImportExportE2eIds.ARTICLE_IMPORT_SUCCESS,
            actionTestTag = ImportExportE2eIds.DONE,
        )
        is FeedImportExportState.ImportSuccess -> {
            if (state.notValidFeedSources.isEmpty() && state.feedSourceWithError.isEmpty()) {
                ResultView(
                    message = strings.feedsImportDoneMessage,
                    action = strings.doneButton,
                    onAction = onDoneClick,
                    modifier = modifier,
                    messageTestTag = ImportExportE2eIds.FEED_IMPORT_SUCCESS,
                    actionTestTag = ImportExportE2eIds.DONE,
                )
            } else {
                ImportReport(state, onDoneClick, modifier)
            }
        }
    }
}

@Composable
private fun ImportExportIdleContent(
    enabled: Boolean,
    lastAction: ImportExportAction?,
    focusRevision: Int,
    onAction: (ImportExportAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val listState = rememberLazyListState()
    val focus = remember { ImportExportAction.entries.associateWith { FocusRequester() } }
    val actionHeight = importExportActionHeight()
    LaunchedEffect(enabled, focusRevision) {
        if (enabled) focus.getValue(lastAction ?: ImportExportAction.ImportFeeds).requestFocus()
    }
    ImportExportPanel(
        modifier = modifier,
        footer = {
            DefaultButton(onClick = onClose, enabled = enabled, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(strings.actionDone)
            }
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = DesktopDesignTokens.spaceSection),
            verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spacePanel),
        ) {
            item {
                Text(
                    strings.importExportLabel,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    ),
                )
            }
            item {
                ImportExportSection(
                    title = strings.importExportOpmlSectionTitle,
                    description = strings.importExportDescription,
                ) {
                    ImportExportActionButton(
                        action = ImportExportAction.ImportFeeds,
                        enabled = enabled,
                        focus = focus.getValue(ImportExportAction.ImportFeeds),
                        onAction = onAction,
                    )
                    ImportExportActionButton(
                        action = ImportExportAction.ExportFeeds,
                        enabled = enabled,
                        focus = focus.getValue(ImportExportAction.ExportFeeds),
                        onAction = onAction,
                    )
                }
            }
            item {
                ImportExportSection(
                    title = strings.importExportArticlesSectionTitle,
                    description = strings.importExportArticlesDescription,
                ) {
                    ImportExportActionButton(
                        action = ImportExportAction.ImportArticles,
                        enabled = enabled,
                        focus = focus.getValue(ImportExportAction.ImportArticles),
                        onAction = onAction,
                    )
                    ImportExportActionButton(
                        action = ImportExportAction.ExportArticles,
                        enabled = enabled,
                        focus = focus.getValue(ImportExportAction.ExportArticles),
                        onAction = onAction,
                    )
                }
            }
        }
        if (listState.canScrollForward || listState.canScrollBackward) {
            FeedFlowVerticalScrollbar(rememberScrollbarState(listState))
        }
    }
}

@Composable
private fun ImportExportSection(title: String, description: String, actions: @Composable () -> Unit) {
    SettingsSection(title = title, footer = description) {
        SettingsBlock {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
            ) { actions() }
        }
    }
}

@Composable
private fun ImportExportActionButton(
    action: ImportExportAction,
    enabled: Boolean,
    focus: FocusRequester,
    onAction: (ImportExportAction) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val (label, tag) = when (action) {
        ImportExportAction.ImportFeeds -> strings.importFeedButton to ImportExportE2eIds.IMPORT_FEEDS
        ImportExportAction.ExportFeeds -> strings.exportFeedsButton to ImportExportE2eIds.EXPORT_FEEDS
        ImportExportAction.ImportArticles -> strings.importArticlesButton to ImportExportE2eIds.IMPORT_ARTICLES
        ImportExportAction.ExportArticles -> strings.exportArticlesButton to ImportExportE2eIds.EXPORT_ARTICLES
    }
    OutlinedButton(
        onClick = { onAction(action) },
        enabled = enabled,
        modifier = Modifier.heightIn(min = importExportActionHeight())
            .focusRequester(focus).testTag(tag),
    ) { Text(label) }
}

@Composable
internal fun ArticleExportFilterDialog(
    filter: ArticleExportFilter,
    onFilterChange: (ArticleExportFilter) -> Unit,
    onDismiss: () -> Unit,
    onExport: (ArticleExportFilter) -> Unit,
) {
    var settled by remember { mutableStateOf(false) }
    val currentFilter by rememberUpdatedState(filter)
    val dismiss by rememberUpdatedState(onDismiss)
    val export by rememberUpdatedState(onExport)
    val close: () -> Unit = {
        if (!settled) {
            settled = true
            dismiss()
        }
    }
    Dialog(
        title = LocalFeedFlowStrings.current.articlesExportFilterTitle,
        size = DpSize(440.dp, 430.dp),
        onCloseRequest = close,
    ) {
        ArticleExportFilterContent(
            filter = filter,
            onFilterChange = { if (!settled) onFilterChange(it) },
            onDismiss = close,
            onExport = {
                if (!settled) {
                    settled = true
                    export(currentFilter)
                }
            },
            enabled = !settled,
        )
    }
}

@Composable
internal fun ArticleExportFilterContent(
    filter: ArticleExportFilter,
    onFilterChange: (ArticleExportFilter) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val strings = LocalFeedFlowStrings.current
    val focus = remember { ArticleExportFilter.entries.associateWith { FocusRequester() } }
    val scrollState = rememberScrollState()
    val actionHeight = importExportActionHeight()
    LaunchedEffect(Unit) { if (enabled) focus.getValue(filter).requestFocus() }
    ImportExportPanel(
        modifier = modifier,
        footer = {
            OutlinedButton(onClick = onDismiss, enabled = enabled, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(strings.cancelButton)
            }
            DefaultButton(onClick = onExport, enabled = enabled, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(strings.confirmButton)
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(end = DesktopDesignTokens.spaceSection)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
        ) {
            Text(strings.articlesExportFilterDescription)
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
            ) {
                ArticleExportFilter.entries.forEach { option ->
                    RadioButtonRow(
                        text = option.label(),
                        selected = option == filter,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight)
                            .focusRequester(focus.getValue(option)),
                        onClick = { if (enabled) onFilterChange(option) },
                    )
                }
            }
        }
        FeedFlowVerticalScrollbar(rememberScrollbarState(scrollState))
    }
}

@Composable
private fun ImportReport(
    state: FeedImportExportState.ImportSuccess,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val listState = rememberLazyListState()
    val actionFocus = remember { FocusRequester() }
    var handled by remember { mutableStateOf(false) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) { actionFocus.requestFocus() }
    ImportExportPanel(
        modifier = modifier,
        footer = {
            DefaultButton(
                onClick = {
                    if (!handled) {
                        handled = true
                        done()
                    }
                },
                enabled = !handled,
                modifier = Modifier.heightIn(min = importExportActionHeight()).focusRequester(actionFocus)
                    .testTag(ImportExportE2eIds.DONE),
            ) { Text(strings.doneButton) }
        },
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = DesktopDesignTokens.spaceSection),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
            ) {
                if (state.notValidFeedSources.isNotEmpty()) {
                    item(key = "invalid-heading") { ReportHeading(strings.wrongLinkReportTitle) }
                    itemsIndexed(
                        items = state.notValidFeedSources,
                        key = { index, source -> "invalid-$index-${source.id}" },
                    ) { _, source ->
                        ReportSource(source)
                    }
                }
                if (state.feedSourceWithError.isNotEmpty()) {
                    item(key = "error-heading") { ReportHeading(strings.linkWithErrorReportTitle) }
                    itemsIndexed(
                        items = state.feedSourceWithError,
                        key = { index, source -> "error-$index-${source.id}" },
                    ) { _, source ->
                        ReportSource(source)
                    }
                }
            }
        }
        FeedFlowVerticalScrollbar(rememberScrollbarState(listState))
    }
}

@Composable
private fun ReportHeading(text: String) {
    Text(text, style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.SemiBold))
}

@Composable
private fun ReportSource(source: ParsedFeedSource) {
    Column(verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSmall)) {
        Text(source.title.ifBlank { source.url }, fontWeight = FontWeight.Medium)
        if (source.title.isNotBlank()) Text(source.url, color = JewelTheme.globalColors.text.info)
    }
}

@Composable
private fun ResultView(
    message: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    messageTestTag: String? = null,
    actionTestTag: String? = null,
) {
    var handled by remember(messageTestTag) { mutableStateOf(false) }
    val activate by rememberUpdatedState(onAction)
    val focus = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    LaunchedEffect(messageTestTag) { focus.requestFocus() }
    ImportExportPanel(
        modifier = modifier,
        footer = {
            DefaultButton(
                onClick = {
                    if (!handled) {
                        handled = true
                        activate()
                    }
                },
                enabled = !handled,
                modifier = Modifier.heightIn(min = importExportActionHeight()).focusRequester(focus)
                    .then(actionTestTag?.let { Modifier.testTag(it) } ?: Modifier),
            ) { Text(action) }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                modifier = messageTestTag?.let { Modifier.testTag(it) } ?: Modifier,
                style = DesktopDesignTokens.sectionHeading,
                textAlign = TextAlign.Center,
            )
        }
        FeedFlowVerticalScrollbar(rememberScrollbarState(scrollState))
    }
}

@Composable
private fun ImportExportPanel(
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(DesktopDesignTokens.spacePanel),
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), content = body)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
        ) { footer() }
    }
}

@Composable
private fun importExportActionHeight(): Dp =
    with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp

@Composable
private fun ArticleExportFilter.label(): String = when (this) {
    ArticleExportFilter.All -> LocalFeedFlowStrings.current.articlesExportFilterAll
    ArticleExportFilter.Read -> LocalFeedFlowStrings.current.articlesExportFilterRead
    ArticleExportFilter.Unread -> LocalFeedFlowStrings.current.articlesExportFilterUnread
    ArticleExportFilter.Bookmarked -> LocalFeedFlowStrings.current.articlesExportFilterBookmarked
}

private fun dispatchFileAction(
    action: ImportExportAction,
    importFeeds: () -> Unit,
    exportFeeds: () -> Unit,
    importArticles: () -> Unit,
) {
    when (action) {
        ImportExportAction.ImportFeeds -> importFeeds()
        ImportExportAction.ExportFeeds -> exportFeeds()
        ImportExportAction.ImportArticles -> importArticles()
        ImportExportAction.ExportArticles -> Unit
    }
}

private enum class ImportExportAction {
    ImportFeeds,
    ExportFeeds,
    ImportArticles,
    ExportArticles,
}
