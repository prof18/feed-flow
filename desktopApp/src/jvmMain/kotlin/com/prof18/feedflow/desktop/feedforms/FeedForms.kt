package com.prof18.feedflow.desktop.feedforms

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.YouTubeOptionsState
import com.prof18.feedflow.desktop.settings.SettingsCheckbox
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.desktop.ui.components.SnackbarHost
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

internal const val ForceAddFeedTag = "force-add-feed"

@OptIn(ExperimentalJewelApi::class)
@Composable
internal fun AddFeedForm(
    feedUrl: String,
    showError: Boolean,
    showLoading: Boolean,
    errorMessage: String,
    categoriesState: CategoriesState,
    showNotificationToggle: Boolean,
    isNotificationEnabled: Boolean,
    canForceAdd: Boolean,
    onFeedUrlUpdated: (String) -> Unit,
    onAddFeed: () -> Unit,
    onCategorySelectorClick: () -> Unit,
    onNotificationToggleChanged: (Boolean) -> Unit,
    onForceAddFeed: () -> Unit,
    modifier: Modifier = Modifier,
    youTubeOptions: YouTubeOptionsState = YouTubeOptionsState(),
    onHideShortsToggleChanged: (Boolean) -> Unit = {},
    completionRevision: Int = 0,
    statusMessage: String? = null,
    topAppBar: @Composable () -> Unit = {},
    snackbarHostState: DesktopFeedbackState? = null,
) {
    val strings = LocalFeedFlowStrings.current
    var urlValue by remember { mutableStateOf(TextFieldValue(feedUrl)) }
    LaunchedEffect(feedUrl) {
        urlValue = urlValue.synchronizeExternalText(feedUrl)
    }
    var forceAcknowledged by remember(showError) { mutableStateOf(false) }
    val submission = rememberFeedSubmissionState(showLoading, completionRevision)
    val urlFocus = rememberFeedFieldFocus(submission.busy, completionRevision)
    val latestForceRequired by rememberUpdatedState(showError && canForceAdd)
    val latestAdd by rememberUpdatedState(onAddFeed)
    val latestForceAdd by rememberUpdatedState(onForceAddFeed)
    val submit = {
        submission.submitAddFeed(urlValue.text, latestForceRequired, forceAcknowledged, latestAdd, latestForceAdd)
    }
    val forceRequired = showError && canForceAdd
    val actionHeight = feedFormActionHeight()
    Box(modifier = modifier.fillMaxSize()) {
        FeedFormLayout(
            topAppBar = topAppBar,
            body = {
                FeedFormTextField(
                    label = strings.feedUrl,
                    value = urlValue,
                    hint = strings.feedUrlPlaceholder,
                    tag = FeedFormE2eIds.ADD_FEED_URL_INPUT,
                    enabled = !submission.busy,
                    focus = urlFocus,
                    onValueChange = { value ->
                        if (!submission.busy) {
                            urlValue = value
                            onFeedUrlUpdated(value.text)
                        }
                    },
                    onSubmit = submit,
                )
                Text(strings.feedUrlHelpText)
                FeedFormError(errorMessage.takeIf { showError })
                FeedCategorySelector(categoriesState, !submission.busy) {
                    if (!submission.busy) onCategorySelectorClick()
                }
                if (showNotificationToggle) {
                    CheckboxRow(
                        text = strings.enableNotificationsForFeed,
                        checked = isNotificationEnabled,
                        onCheckedChange = { if (!submission.busy) onNotificationToggleChanged(it) },
                        enabled = !submission.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HideShortsOption(youTubeOptions, enabled = !submission.busy, onToggle = onHideShortsToggleChanged)
                statusMessage?.let { Text(it) }
                if (forceRequired) {
                    CheckboxRow(
                        text = strings.addFeedAnywayAcknowledgement,
                        checked = forceAcknowledged,
                        onCheckedChange = { if (!submission.busy) forceAcknowledged = it },
                        enabled = !submission.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            actions = {
                DefaultButton(
                    onClick = submit,
                    modifier = Modifier.heightIn(min = actionHeight)
                        .then(if (forceRequired) Modifier.testTag(ForceAddFeedTag) else Modifier),
                    enabled = urlValue.text.isNotBlank() && !submission.busy && (!forceRequired || forceAcknowledged),
                ) {
                    FeedSubmitLabel(if (forceRequired) strings.addFeedAnywayButton else strings.addFeed, showLoading)
                }
            },
        )
        snackbarHostState?.let { SnackbarHost(it, Modifier.align(Alignment.BottomCenter)) }
    }
}

@Composable
private fun HideShortsOption(options: YouTubeOptionsState, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    if (!options.showHideShortsToggle) return
    val strings = LocalFeedFlowStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CheckboxRow(
            text = strings.hideYoutubeShorts,
            checked = options.hideShorts,
            onCheckedChange = { if (enabled) onToggle(it) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag(FeedFormE2eIds.ADD_FEED_HIDE_YOUTUBE_SHORTS),
        )
        Text(strings.hideYoutubeShortsDescription, color = JewelTheme.globalColors.text.info)
    }
}

@OptIn(ExperimentalJewelApi::class)
@Composable
internal fun EditFeedForm(
    feedUrl: String,
    feedName: String,
    showError: Boolean,
    showLoading: Boolean,
    errorMessage: String,
    canEditUrl: Boolean,
    categoriesState: CategoriesState,
    feedSourceSettings: FeedSourceSettings,
    onFeedUrlUpdated: (String) -> Unit,
    onFeedNameUpdated: (String) -> Unit,
    articleOpenModeSelector: @Composable (Modifier, Boolean) -> Unit,
    onHiddenToggled: (Boolean) -> Unit,
    onHideImagesToggled: (Boolean) -> Unit,
    onPinnedToggled: (Boolean) -> Unit,
    showNotificationToggle: Boolean,
    onNotificationToggleChanged: (Boolean) -> Unit,
    onCategorySelectorClick: () -> Unit,
    editFeed: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    showDeleteDialog: Boolean,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
    completionRevision: Int = 0,
) {
    val strings = LocalFeedFlowStrings.current
    var nameValue by remember { mutableStateOf(TextFieldValue(feedName)) }
    var urlValue by remember { mutableStateOf(TextFieldValue(feedUrl)) }
    LaunchedEffect(feedName) {
        nameValue = nameValue.synchronizeExternalText(feedName)
    }
    LaunchedEffect(feedUrl) {
        urlValue = urlValue.synchronizeExternalText(feedUrl)
    }
    val submission = rememberFeedSubmissionState(showLoading, completionRevision)
    val nameFocus = rememberFeedFieldFocus(submission.busy, completionRevision)
    val latestEdit by rememberUpdatedState(editFeed)
    val submit = { submission.submit(nameValue.text.isNotEmpty() && urlValue.text.isNotBlank(), latestEdit) }
    val actionHeight = feedFormActionHeight()
    FeedFormLayout(
        modifier = modifier,
        body = {
            FeedFormTextField(
                label = strings.feedName,
                value = nameValue,
                hint = strings.feedName,
                tag = FeedFormE2eIds.NAME_INPUT,
                enabled = !submission.busy,
                focus = nameFocus,
                onValueChange = {
                    if (!submission.busy) {
                        nameValue = it
                        onFeedNameUpdated(it.text)
                    }
                },
                onSubmit = submit,
            )
            if (canEditUrl) {
                FeedFormTextField(
                    label = strings.feedUrl,
                    value = urlValue,
                    hint = strings.feedUrlPlaceholder,
                    tag = FeedFormE2eIds.ADD_FEED_URL_INPUT,
                    enabled = !submission.busy,
                    onValueChange = {
                        if (!submission.busy) {
                            urlValue = it
                            onFeedUrlUpdated(it.text)
                        }
                    },
                    onSubmit = submit,
                )
            }
            FeedFormError(errorMessage.takeIf { showError })
            FeedCategorySelector(categoriesState, !submission.busy) {
                if (!submission.busy) onCategorySelectorClick()
            }
            articleOpenModeSelector(Modifier.fillMaxWidth(), !submission.busy)
            SettingsCheckbox(
                title = strings.hideFeedFromTimelineDescription,
                checked = feedSourceSettings.isHiddenFromTimeline,
                onCheckedChange = { if (!submission.busy) onHiddenToggled(it) },
                enabled = !submission.busy,
            )
            SettingsCheckbox(
                title = strings.hideImagesForFeedDescription,
                checked = feedSourceSettings.isHideImagesEnabled,
                onCheckedChange = { if (!submission.busy) onHideImagesToggled(it) },
                enabled = !submission.busy,
            )
            SettingsCheckbox(
                title = strings.pinFeedSourceDescription,
                checked = feedSourceSettings.isPinned,
                onCheckedChange = { if (!submission.busy) onPinnedToggled(it) },
                enabled = !submission.busy,
            )
            if (showNotificationToggle) {
                SettingsCheckbox(
                    title = strings.enableNotificationsForFeed,
                    checked = feedSourceSettings.isNotificationEnabled,
                    onCheckedChange = { if (!submission.busy) onNotificationToggleChanged(it) },
                    enabled = !submission.busy,
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = { if (!submission.busy) onShowDeleteDialog() },
                modifier = Modifier.heightIn(min = actionHeight),
                enabled = !submission.busy,
            ) { Text(strings.deleteFeedButton) }
            DefaultButton(
                onClick = submit,
                modifier = Modifier.heightIn(min = actionHeight),
                enabled = nameValue.text.isNotEmpty() && urlValue.text.isNotBlank() && !submission.busy,
            ) { FeedSubmitLabel(strings.actionSave, showLoading) }
        },
    )
    DeleteFeedConfirmation(showDeleteDialog, onDismissDeleteDialog, onConfirmDelete)
}

@Composable
private fun FeedFormLayout(
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    topAppBar: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        topAppBar()
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = body,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { actions() }
    }
}

@OptIn(ExperimentalJewelApi::class)
@Composable
private fun FeedFormTextField(
    label: String,
    value: TextFieldValue,
    hint: String,
    tag: String,
    enabled: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    focus: FocusRequester? = null,
) {
    val latestChange by rememberUpdatedState(onValueChange)
    val latestSubmit by rememberUpdatedState(onSubmit)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        TextField(
            value = value,
            onValueChange = { latestChange(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = feedFormActionHeight()).testTag(tag)
                .then(focus?.let { Modifier.focusRequester(it) } ?: Modifier)
                .semantics { contentDescription = label },
            enabled = enabled,
            placeholder = { Text(hint) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { latestSubmit() }),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FeedCategorySelector(state: CategoriesState, enabled: Boolean, onClick: () -> Unit) {
    val strings = LocalFeedFlowStrings.current
    val name = state.categories.firstOrNull { it.isSelected }?.name ?: strings.noCategorySelectedHeader
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.addFeedCategoryTitle)
        Tooltip(tooltip = { Text(name) }) {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = feedFormActionHeight())
                    .semantics { contentDescription = "${strings.addFeedCategoryTitle}: $name" },
            ) { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun FeedFormError(message: String?) {
    if (!message.isNullOrBlank()) Text(message, modifier = Modifier.semantics { error(message) })
}

@Composable
private fun FeedSubmitLabel(label: String, loading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(width = 16.dp, height = 16.dp))
        Text(label)
    }
}

@Composable
private fun feedFormActionHeight(): Dp = with(LocalDensity.current) {
    JewelTheme.defaultTextStyle.lineHeight.toDp()
} + 16.dp

private fun FeedSubmissionState.submitAddFeed(
    url: String,
    forceRequired: Boolean,
    forceAcknowledged: Boolean,
    onAdd: () -> Unit,
    onForce: () -> Unit,
) {
    submit(url.isNotBlank() && (!forceRequired || forceAcknowledged)) {
        if (forceRequired) onForce() else onAdd()
    }
}

private fun TextFieldValue.synchronizeExternalText(text: String): TextFieldValue {
    if (this.text == text) return this
    val clampedSelection = TextRange(
        start = selection.start.coerceIn(0, text.length),
        end = selection.end.coerceIn(0, text.length),
    )
    return TextFieldValue(text = text, selection = clampedSelection)
}

@Composable
internal fun DeleteFeedConfirmation(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    val strings = LocalFeedFlowStrings.current
    var deleting by remember(visible) { mutableStateOf(false) }
    Dialog(title = strings.deleteFeedConfirmationTitle, onCloseRequest = { if (!deleting) onDismiss() }) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.deleteFeedConfirmationTitle)
            Text(strings.deleteFeedConfirmationMessage)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, enabled = !deleting) {
                    Text(strings.deleteCategoryCloseButton)
                }
                DefaultButton(
                    onClick = {
                        if (!deleting) {
                            deleting = true
                            onConfirm()
                        }
                    },
                    enabled = !deleting,
                ) {
                    if (deleting) CircularProgressIndicator() else Text(strings.deleteFeed)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun CategoryEditorDialog(
    categoryState: CategoriesState,
    onCategorySelected: (CategoryId) -> Unit,
    onAddCategory: (CategoryName) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
    onEditCategory: (CategoryId, CategoryName) -> Unit,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onDismiss: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val categoryActionsEnabled = !categoryState.isLoading
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoriesState.CategoryItem?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryId?>(null) }
    val actionHeight = feedFormActionHeight()
    Dialog(title = strings.addFeedCategoriesTitle, onCloseRequest = onDismiss) {
        FeedFormLayout(
            body = {
                categoryState.categories.forEach { category ->
                    val name = category.name ?: strings.noCategorySelectedHeader
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Tooltip(modifier = Modifier.weight(1f), tooltip = { Text(name) }) {
                            CheckboxRow(
                                text = name,
                                checked = category.isSelected,
                                onCheckedChange = {
                                    if (!categoryState.isLoading) onCategorySelected(CategoryId(category.id))
                                },
                                modifier = Modifier.fillMaxWidth().testTag(FeedFormE2eIds.categoryChip(name)),
                                enabled = categoryActionsEnabled,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (category.name != null) {
                            Tooltip(tooltip = { Text(strings.editCategory) }) {
                                IconButton(
                                    onClick = { if (!categoryState.isLoading) editingCategory = category },
                                    enabled = categoryActionsEnabled,
                                    modifier = Modifier.size(actionHeight)
                                        .testTag(categoryActionTag(category.id, "edit"))
                                        .semantics { contentDescription = strings.editCategory },
                                ) { Icon(AllIconsKeys.Actions.Edit, null) }
                            }
                            Tooltip(tooltip = { Text(strings.deleteCategory) }) {
                                IconButton(
                                    onClick = {
                                        if (!categoryState.isLoading) deletingCategory = CategoryId(category.id)
                                    },
                                    enabled = categoryActionsEnabled,
                                    modifier = Modifier.size(actionHeight)
                                        .testTag(categoryActionTag(category.id, "delete"))
                                        .semantics { contentDescription = strings.deleteCategory },
                                ) { Icon(AllIconsKeys.General.Delete, null) }
                            }
                        }
                    }
                }
                if (categoryState.isLoading) CircularProgressIndicator()
            },
            actions = {
                OutlinedButton(
                    onClick = { if (!categoryState.isLoading) showAddDialog = true },
                    modifier = Modifier.heightIn(min = actionHeight).testTag(FeedFormE2eIds.CATEGORY_SHEET_ADD),
                    enabled = categoryActionsEnabled,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(AllIconsKeys.General.Add, null)
                        Text(strings.addFeedCategoryTitle)
                    }
                }
                DefaultButton(onClick = onDismiss, modifier = Modifier.heightIn(min = actionHeight)) {
                    Text(strings.actionDone)
                }
            },
        )
        CategoryNameDialog(
            visible = showAddDialog,
            title = strings.addFeedCategoryTitle,
            initialName = "",
            categoryId = null,
            validateCategoryName = validateCategoryName,
            enabled = categoryActionsEnabled,
            onDismiss = { showAddDialog = false },
            onSave = { name ->
                if (!categoryState.isLoading) {
                    onAddCategory(name)
                    showAddDialog = false
                }
            },
        )
        editingCategory?.let { category ->
            CategoryNameDialog(
                visible = true,
                title = strings.editCategory,
                initialName = category.name.orEmpty(),
                categoryId = CategoryId(category.id),
                validateCategoryName = validateCategoryName,
                enabled = categoryActionsEnabled,
                onDismiss = { editingCategory = null },
                onSave = { name ->
                    if (!categoryState.isLoading) {
                        onEditCategory(CategoryId(category.id), name)
                        editingCategory = null
                    }
                },
            )
        }
        deletingCategory?.let { categoryId ->
            ConfirmationDialog(
                title = strings.deleteCategoryConfirmationTitle,
                message = strings.deleteCategoryConfirmationMessage,
                confirmLabel = strings.deleteCategory,
                onConfirm = {
                    if (!categoryState.isLoading) onDeleteCategory(categoryId)
                    deletingCategory = null
                },
                onDismiss = { deletingCategory = null },
            )
        }
    }
}

@OptIn(ExperimentalJewelApi::class)
@Composable
internal fun CategoryNameDialog(
    visible: Boolean,
    title: String,
    initialName: String,
    categoryId: CategoryId?,
    validateCategoryName: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
    onDismiss: () -> Unit,
    onSave: (CategoryName) -> Unit,
    enabled: Boolean = true,
) {
    if (!visible) return
    val strings = LocalFeedFlowStrings.current
    var value by remember(categoryId) { mutableStateOf(TextFieldValue(initialName)) }
    var settled by remember(categoryId) { mutableStateOf(false) }
    val focus = remember(categoryId) { FocusRequester() }
    val latestEnabled by rememberUpdatedState(enabled)
    val latestSave by rememberUpdatedState(onSave)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestValidate by rememberUpdatedState(validateCategoryName)
    val categoryName = CategoryName(value.text)
    val validationResult = validateCategoryName(categoryId, categoryName)
    val valid = validationResult == CategoryNameValidationResult.VALID
    val duplicate = validationResult == CategoryNameValidationResult.DUPLICATE
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    val save = {
        if (canSaveCategoryName(latestEnabled, settled, categoryId, value.text, latestValidate)) {
            settled = true
            latestSave(CategoryName(value.text.trim()))
        }
    }
    val dismiss = {
        if (!settled) {
            settled = true
            latestDismiss()
        }
    }
    Dialog(title = title, onCloseRequest = dismiss) {
        LaunchedEffect(categoryId) { focus.requestFocus() }
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(strings.newCategoryHint)
                TextField(
                    value = value,
                    onValueChange = { if (latestEnabled && !settled) value = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight).focusRequester(focus).testTag(
                        if (categoryId == null) {
                            FeedFormE2eIds.CATEGORY_ADD_INPUT
                        } else {
                            FeedFormE2eIds.CATEGORY_RENAME_INPUT
                        },
                    ).semantics {
                        contentDescription = strings.newCategoryHint
                        if (duplicate) error(strings.categoryNameAlreadyExists)
                    },
                    enabled = enabled && !settled,
                    outline = if (duplicate) Outline.Error else Outline.None,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                )
                if (duplicate) Text(strings.categoryNameAlreadyExists)
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = dismiss,
                    enabled = !settled,
                    modifier = Modifier.heightIn(min = actionHeight),
                ) {
                    Text(strings.cancelButton)
                }
                DefaultButton(
                    onClick = save,
                    enabled = valid && enabled && !settled,
                    modifier = Modifier.heightIn(min = actionHeight).testTag(
                        if (categoryId == null) {
                            FeedFormE2eIds.CATEGORY_ADD_CONFIRM
                        } else {
                            FeedFormE2eIds.CATEGORY_RENAME_SAVE
                        },
                    ),
                ) { Text(if (categoryId == null) strings.confirmButton else strings.actionSave) }
            }
        }
    }
}

private fun categoryActionTag(categoryId: String, action: String): String =
    "edit_feed_category_${action}_$categoryId"

private fun canSaveCategoryName(
    enabled: Boolean,
    settled: Boolean,
    categoryId: CategoryId?,
    name: String,
    validate: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
): Boolean = enabled && !settled && validate(categoryId, CategoryName(name)) == CategoryNameValidationResult.VALID
