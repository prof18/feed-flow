package com.prof18.feedflow.desktop.about

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.useResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.SimpleListItem
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@Composable
fun LicensesScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val dispatchers = remember { DI.koin.get<DispatcherProvider>() }
    val report by produceState<Result<Libs>?>(initialValue = null) {
        value = withContext(dispatchers.default) {
            runCatching {
                val json = useResource("aboutlibraries.json") { it.bufferedReader().readText() }
                Libs.Builder().withJson(json).build()
            }
        }
    }
    LicensesContent(report?.getOrNull(), onBackClick, modifier, loadFailed = report?.isFailure == true)
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun LicensesContent(
    libraries: Libs?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    loadFailed: Boolean = false,
) {
    val strings = LocalFeedFlowStrings.current
    val listState = rememberLazyListState()
    var initialFocusGiven by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<Library?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedFocus by remember { mutableStateOf<FocusRequester?>(null) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    LaunchedEffect(details) {
        if (details == null) selectedFocus?.requestFocus()
    }
    Column(
        modifier = modifier.fillMaxSize().padding(DesktopDesignTokens.spacePanel),
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
    ) {
        if (details == null) {
            Text(
                strings.openSourceNavBar,
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                ),
            )
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (libraries == null && !loadFailed) {
                    CircularProgressIndicator()
                } else if (libraries == null || libraries.libraries.isEmpty()) {
                    Text(strings.readerModeNoContentTitle)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = DesktopDesignTokens.spaceSection)
                            .testTag(DesktopLicensesE2eIds.LIST),
                    ) {
                        itemsIndexed(libraries.libraries, key = { _, library -> library.uniqueId }) { index, library ->
                            val focus = focusRequesters.getOrPut(library.uniqueId) { FocusRequester() }
                            LaunchedEffect(Unit) {
                                if (index == 0 && !initialFocusGiven) {
                                    focus.requestFocus()
                                    initialFocusGiven = true
                                }
                            }
                            val label = listOfNotNull(library.name, library.artifactVersion).joinToString(" · ")
                            val accessibleLabel = "$label\n${library.uniqueId}"
                            Tooltip(tooltip = { Text(accessibleLabel) }) {
                                SimpleListItem(
                                    text = label,
                                    state = ListItemState(isSelected = selectedId == library.uniqueId, isActive = true),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focus)
                                        .testTag(DesktopLicensesE2eIds.row(library.uniqueId))
                                        .semantics {
                                            selected = selectedId == library.uniqueId
                                            contentDescription = accessibleLabel
                                        }
                                        .clickable {
                                            selectedId = library.uniqueId
                                            selectedFocus = focus
                                            details = library
                                        },
                                    height = actionHeight,
                                )
                            }
                        }
                    }
                    if (listState.canScrollForward || listState.canScrollBackward) {
                        FeedFlowVerticalScrollbar(listState)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                DefaultButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = actionHeight).testTag(DesktopLicensesE2eIds.LIST_DONE),
                ) {
                    Text(strings.actionDone)
                }
            }
        } else {
            LicenseDetails(
                library = requireNotNull(details),
                onClose = { details = null },
                modifier = Modifier.testTag(DesktopLicensesE2eIds.DETAILS),
            )
        }
    }
}

@Composable
internal fun LicenseDetails(library: Library, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalFeedFlowStrings.current
    val scrollState = rememberScrollState()
    val closeFocus = remember { FocusRequester() }
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    LaunchedEffect(Unit) { closeFocus.requestFocus() }
    Column(
        modifier = modifier.fillMaxSize().padding(DesktopDesignTokens.spacePanel),
        verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
    ) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                        .padding(end = DesktopDesignTokens.spaceSection),
                    verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
                ) {
                    Text(listOfNotNull(library.name, library.artifactVersion).joinToString(" · "))
                    Text(
                        library.uniqueId,
                        modifier = Modifier.testTag(DesktopLicensesE2eIds.ARTIFACT_ID),
                        style = DesktopDesignTokens.metadataText,
                    )
                    if (library.licenses.isEmpty()) {
                        Text(strings.readerModeNoContentTitle)
                    }
                    library.licenses.forEach { license ->
                        Text(license.name)
                        Text(license.licenseContent?.takeIf(String::isNotBlank) ?: strings.readerModeNoContentTitle)
                    }
                }
            }
            FeedFlowVerticalScrollbar(scrollState)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            DefaultButton(
                onClick = onClose,
                modifier = Modifier.heightIn(min = actionHeight).focusRequester(closeFocus)
                    .testTag(DesktopLicensesE2eIds.DETAILS_DONE),
            ) { Text(strings.desktopSearchBackButton) }
        }
    }
}

internal object DesktopLicensesE2eIds {
    const val LIST = "desktop_licenses_list"
    const val DETAILS = "desktop_license_details"
    const val LIST_DONE = "desktop_licenses_done"
    const val DETAILS_DONE = "desktop_license_details_done"
    const val ARTIFACT_ID = "desktop_license_artifact_id"

    fun row(uniqueId: String): String = "desktop_license_$uniqueId"
}
