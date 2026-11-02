package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.canonical
import com.prof18.feedflow.core.model.canonicalCategoryName
import com.prof18.feedflow.desktop.feedforms.CategoryNameDialog
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopChoiceDropdown
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.Dialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun DesktopFeedMoveDialog(
    categories: ImmutableList<FeedSourceCategory>,
    feedSources: ImmutableList<FeedSource>,
    onMove: (List<FeedSource>, FeedSourceCategory?) -> Unit,
    onCreateCategory: (CategoryName) -> Unit,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onDismiss: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    var selectedId by remember { mutableStateOf(DesktopDrawerUncategorizedSectionKey) }
    var showNewCategory by remember { mutableStateOf(false) }
    var pendingCategoryName by remember { mutableStateOf<CategoryName?>(null) }

    // A category created from the picker becomes the destination as soon as the drawer reports it.
    val createdCategory = pendingCategoryName?.let { name ->
        categories.firstOrNull { it.title.canonicalCategoryName() == name.canonical() }
    }
    val destinationId = createdCategory?.id ?: selectedId
    val selectedCategory = categories.firstOrNull { it.id == destinationId }
    val sourcesToMove = feedSources.filter { it.category?.id != selectedCategory?.id }
    val choices = (
        listOf(DesktopDrawerUncategorizedSectionKey to strings.noCategory) +
            categories.map { it.id to it.title } +
            (NEW_CATEGORY_KEY to strings.desktopNewCategoryMenu)
        ).toPersistentList()

    Dialog(
        title = strings.changeCategory,
        onCloseRequest = onDismiss,
        size = MoveDialogSize,
        resizable = false,
    ) {
        val destinationFocus = remember { FocusRequester() }
        val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
        LaunchedEffect(Unit) { destinationFocus.requestFocus() }
        Column(
            Modifier.fillMaxSize().padding(DesktopDesignTokens.spacePanel),
            verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceSection),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
            ) {
                Text(strings.desktopFeedSelectionCount(feedSources.size))
                DesktopChoiceDropdown(
                    title = strings.changeCategory,
                    choices = choices,
                    selected = destinationId,
                    onSelected = { id ->
                        if (id == NEW_CATEGORY_KEY) {
                            showNewCategory = true
                        } else {
                            pendingCategoryName = null
                            selectedId = id
                        }
                    },
                    maxPopupWidth = MovePopupMaxWidth,
                    modifier = Modifier.fillMaxWidth().focusRequester(destinationFocus),
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(DesktopDesignTokens.spaceMedium),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = actionHeight)) {
                    Text(strings.cancelButton)
                }
                DefaultButton(
                    onClick = { onMove(sourcesToMove, selectedCategory) },
                    enabled = sourcesToMove.isNotEmpty(),
                    modifier = Modifier.heightIn(min = actionHeight),
                ) { Text(strings.confirmButton) }
            }
        }
    }

    CategoryNameDialog(
        visible = showNewCategory,
        title = strings.desktopNewCategoryMenu.trimEnd('…', '.'),
        initialName = "",
        categoryId = null,
        validateCategoryName = validateCategoryName,
        onDismiss = { showNewCategory = false },
        onSave = { name ->
            onCreateCategory(name)
            pendingCategoryName = name
            showNewCategory = false
        },
    )
}

private val MoveDialogSize = DpSize(width = 380.dp, height = 200.dp)
private val MovePopupMaxWidth = MoveDialogSize.width - DesktopDesignTokens.spacePanel * 2

private const val NEW_CATEGORY_KEY = "feedflow-new-category"
