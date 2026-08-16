package com.prof18.feedflow.android.widget

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun WidgetSettingsScaffold(
    title: String,
    settingsState: WidgetSettingsState,
    onFeedLayoutSelected: (WidgetFeedLayout) -> Unit,
    onShowHeaderSelected: (Boolean) -> Unit,
    onFontScaleSelected: (Int) -> Unit,
    onBackgroundColorSelected: (Int?) -> Unit,
    onBackgroundOpacitySelected: (Int) -> Unit,
    onTextColorModeSelected: (WidgetTextColorMode) -> Unit,
    onHideImagesSelected: (Boolean) -> Unit,
    onCardSurfaceColorSelected: (Int?) -> Unit,
    onCardSurfaceOpacitySelected: (Int) -> Unit,
    onCardCornerRadiusSelected: (Int) -> Unit,
    onCardItemSeparationSelected: (WidgetCardItemSeparation) -> Unit,
    onCardDividerOpacitySelected: (Int) -> Unit,
    onCardImageSizingSelected: (WidgetCardImageSizing) -> Unit,
    showConfirmButton: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        val strings = LocalFeedFlowStrings.current
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
                WidgetPreviewSection(
                    settingsState = settingsState,
                )
            }
            item {
                Text(
                    text = strings.widgetPreviewNote,
                    modifier = Modifier
                        .padding(horizontal = Spacing.regular)
                        .padding(end = Spacing.large)
                        .padding(bottom = Spacing.small),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                )
            }
            item {
                WidgetSettingsContent(
                    settingsState = settingsState,
                    onFeedLayoutSelected = onFeedLayoutSelected,
                    onShowHeaderSelected = onShowHeaderSelected,
                    onFontScaleSelected = onFontScaleSelected,
                    onBackgroundColorSelected = onBackgroundColorSelected,
                    onBackgroundOpacitySelected = onBackgroundOpacitySelected,
                    onTextColorModeSelected = onTextColorModeSelected,
                    onHideImagesSelected = onHideImagesSelected,
                    onCardSurfaceColorSelected = onCardSurfaceColorSelected,
                    onCardSurfaceOpacitySelected = onCardSurfaceOpacitySelected,
                    onCardCornerRadiusSelected = onCardCornerRadiusSelected,
                    onCardItemSeparationSelected = onCardItemSeparationSelected,
                    onCardDividerOpacitySelected = onCardDividerOpacitySelected,
                    onCardImageSizingSelected = onCardImageSizingSelected,
                    showConfirmButton = showConfirmButton,
                    onConfirm = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Preview
@Composable
private fun WidgetSettingsScaffoldPreview() {
    WidgetSettingsScaffoldToolingPreview(
        settingsState = WidgetSettingsState(
            syncPeriod = SyncPeriod.ONE_HOUR,
            feedLayout = WidgetFeedLayout.CARD,
            showHeader = true,
            fontScale = 0,
            backgroundColor = null,
            backgroundOpacityPercent = 100,
        ),
    )
}

@Preview(
    name = "Large font short landscape",
    widthDp = 640,
    heightDp = 360,
)
@Composable
private fun WidgetSettingsScaffoldLargeFontShortPreview() {
    WidgetSettingsScaffoldToolingPreview(
        settingsState = WidgetSettingsState(
            syncPeriod = SyncPeriod.ONE_HOUR,
            feedLayout = WidgetFeedLayout.CARD,
            showHeader = true,
            fontScale = MAX_WIDGET_FONT_SCALE,
            cardAppearance = WidgetCardAppearance(
                itemSeparation = WidgetCardItemSeparation.DIVIDER,
                imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            ),
        ),
    )
}

@Composable
private fun WidgetSettingsScaffoldToolingPreview(settingsState: WidgetSettingsState) {
    val strings = LocalFeedFlowStrings.current
    FeedFlowTheme {
        WidgetSettingsScaffold(
            title = strings.widgetConfigurationTitle,
            settingsState = settingsState,
            onFeedLayoutSelected = {},
            onShowHeaderSelected = {},
            onFontScaleSelected = {},
            onBackgroundColorSelected = {},
            onBackgroundOpacitySelected = {},
            onTextColorModeSelected = {},
            onHideImagesSelected = {},
            onCardSurfaceColorSelected = {},
            onCardSurfaceOpacitySelected = {},
            onCardCornerRadiusSelected = {},
            onCardItemSeparationSelected = {},
            onCardDividerOpacitySelected = {},
            onCardImageSizingSelected = {},
            showConfirmButton = true,
            onConfirm = {},
            onNavigateBack = {},
        )
    }
}
