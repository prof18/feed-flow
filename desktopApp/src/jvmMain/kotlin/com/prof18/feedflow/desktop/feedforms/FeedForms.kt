package com.prof18.feedflow.desktop.feedforms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.YouTubeOptionsState
import com.prof18.feedflow.desktop.settings.SettingsChoice
import com.prof18.feedflow.desktop.settings.SettingsSection
import com.prof18.feedflow.desktop.settings.SettingsSwitch
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.desktop.ui.components.ProgressButtonLabel
import com.prof18.feedflow.desktop.ui.components.SnackbarHost
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

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
    categoryActions: FeedCategoryActions,
    onNotificationToggleChanged: (Boolean) -> Unit,
    onForceAddFeed: () -> Unit,
    modifier: Modifier = Modifier,
    youTubeOptions: YouTubeOptionsState = YouTubeOptionsState(),
    onHideShortsToggleChanged: (Boolean) -> Unit = {},
    onCancel: (() -> Unit)? = null,
    completionRevision: Int = 0,
    statusMessage: String? = null,
    topAppBar: @Composable () -> Unit = {},
    snackbarHostState: DesktopFeedbackState? = null,
) {
    val strings = LocalFeedFlowStrings.current
    var urlValue by remember { mutableStateOf(TextFieldValue(feedUrl)) }
    SideEffect(feedUrl) {
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
                FeedFormHint(strings.feedUrlHelpText)
                FeedFormError(errorMessage.takeIf { showError })
                SettingsSection {
                    FeedCategoryChoice(categoriesState, !submission.busy, categoryActions)
                    if (showNotificationToggle) {
                        SettingsSwitch(
                            title = strings.enableNotificationsForFeed,
                            checked = isNotificationEnabled,
                            onCheckedChange = { if (!submission.busy) onNotificationToggleChanged(it) },
                            enabled = !submission.busy,
                        )
                    }
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
                onCancel?.let { cancel ->
                    OutlinedButton(
                        onClick = cancel,
                        modifier = Modifier.heightIn(min = actionHeight),
                    ) { Text(strings.cancelButton) }
                }
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
    categoryActions: FeedCategoryActions,
    editFeed: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    showDeleteDialog: Boolean,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
    completionRevision: Int = 0,
    onCancel: (() -> Unit)? = null,
) {
    val strings = LocalFeedFlowStrings.current
    var nameValue by remember { mutableStateOf(TextFieldValue(feedName)) }
    var urlValue by remember { mutableStateOf(TextFieldValue(feedUrl)) }
    SideEffect(feedName) {
        nameValue = nameValue.synchronizeExternalText(feedName)
    }
    SideEffect(feedUrl) {
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
            SettingsSection {
                FeedCategoryChoice(categoriesState, !submission.busy, categoryActions)
                articleOpenModeSelector(Modifier.fillMaxWidth(), !submission.busy)
            }
            SettingsSection {
                SettingsSwitch(
                    title = strings.hideFeedFromTimelineDescription,
                    checked = feedSourceSettings.isHiddenFromTimeline,
                    onCheckedChange = { if (!submission.busy) onHiddenToggled(it) },
                    enabled = !submission.busy,
                )
                SettingsSwitch(
                    title = strings.hideImagesForFeedDescription,
                    checked = feedSourceSettings.isHideImagesEnabled,
                    onCheckedChange = { if (!submission.busy) onHideImagesToggled(it) },
                    enabled = !submission.busy,
                )
                SettingsSwitch(
                    title = strings.pinFeedSourceDescription,
                    checked = feedSourceSettings.isPinned,
                    onCheckedChange = { if (!submission.busy) onPinnedToggled(it) },
                    enabled = !submission.busy,
                )
                if (showNotificationToggle) {
                    SettingsSwitch(
                        title = strings.enableNotificationsForFeed,
                        checked = feedSourceSettings.isNotificationEnabled,
                        onCheckedChange = { if (!submission.busy) onNotificationToggleChanged(it) },
                        enabled = !submission.busy,
                    )
                }
            }
        },
        leadingAction = {
            OutlinedButton(
                onClick = { if (!submission.busy) onShowDeleteDialog() },
                modifier = Modifier.heightIn(min = actionHeight),
                enabled = !submission.busy,
            ) { Text(strings.deleteFeedButton, color = JewelTheme.globalColors.text.error) }
        },
        actions = {
            onCancel?.let { cancel ->
                OutlinedButton(
                    onClick = cancel,
                    modifier = Modifier.heightIn(min = actionHeight),
                ) { Text(strings.cancelButton) }
            }
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
    leadingAction: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        topAppBar()
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = body,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingAction?.invoke()
            Spacer(Modifier.weight(1f))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { actions() }
        }
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

/** Inline category picker with a "New category…" entry, replacing the separate category window. */
@Composable
private fun FeedCategoryChoice(state: CategoriesState, enabled: Boolean, actions: FeedCategoryActions) {
    val strings = LocalFeedFlowStrings.current
    var showNewCategory by remember { mutableStateOf(false) }
    val selectedId = state.categories.firstOrNull { it.isSelected }?.id
        ?: state.categories.firstOrNull { it.name == null }?.id
        ?: NEW_CATEGORY_KEY
    val choices = (
        state.categories.map { it.id to (it.name ?: strings.noCategorySelectedHeader) } +
            (NEW_CATEGORY_KEY to strings.desktopNewCategoryMenu)
        ).toPersistentList()
    SettingsChoice(
        title = strings.addFeedCategoryTitle,
        choices = choices,
        selected = selectedId,
        enabled = enabled && !state.isLoading,
        onSelected = { id ->
            if (id == NEW_CATEGORY_KEY) showNewCategory = true else actions.onSelect(CategoryId(id))
        },
    )
    CategoryNameDialog(
        visible = showNewCategory,
        title = strings.desktopNewCategoryMenu.trimEnd('…', '.'),
        initialName = "",
        categoryId = null,
        validateCategoryName = actions.validate,
        enabled = !state.isLoading,
        onDismiss = { showNewCategory = false },
        onSave = { name ->
            actions.onAdd(name)
            showNewCategory = false
        },
    )
}

@Composable
private fun FeedFormHint(text: String) {
    Text(text, style = DesktopDesignTokens.metadataText, color = JewelTheme.globalColors.text.info)
}

internal data class FeedCategoryActions(
    val onSelect: (CategoryId) -> Unit,
    val onAdd: (CategoryName) -> Unit,
    val validate: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
)

private const val NEW_CATEGORY_KEY = "feedflow-new-category"

@Composable
private fun FeedFormError(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            message,
            color = JewelTheme.globalColors.text.error,
            modifier = Modifier.semantics { error(message) },
        )
    }
}

@Composable
private fun FeedSubmitLabel(label: String, loading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(width = 16.dp, height = 16.dp))
        Text(label)
    }
}

@Composable
@ReadOnlyComposable
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
    Dialog(
        title = strings.deleteFeedConfirmationTitle,
        onCloseRequest = { if (!deleting) onDismiss() },
        alertStyle = true,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.deleteFeedConfirmationTitle, style = DesktopDesignTokens.sectionHeading)
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
                    ProgressButtonLabel(strings.deleteFeed, deleting)
                }
            }
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

private fun canSaveCategoryName(
    enabled: Boolean,
    settled: Boolean,
    categoryId: CategoryId?,
    name: String,
    validate: (CategoryId?, CategoryName) -> CategoryNameValidationResult,
): Boolean = enabled && !settled && validate(categoryId, CategoryName(name)) == CategoryNameValidationResult.VALID
