package com.prof18.feedflow.android.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.android.ui.home.components.NoFeedsInfoContent
import com.prof18.feedflow.android.ui.preview.PreviewPhone
import com.prof18.feedflow.android.ui.style.md_theme_dark_background
import com.prof18.feedflow.android.ui.style.md_theme_light_background
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme

@Composable
internal fun NoFeedsBottomSheet(
    onDismissRequest: () -> Unit,
    onAddFeedClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onAccountsClick: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onFeedSuggestionsClick: () -> Unit = {},
) {
    ModalBottomSheet(
        containerColor = if (isSystemInDarkTheme()) {
            md_theme_dark_background
        } else {
            md_theme_light_background
        },
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        NoFeedsInfoContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onDismissRequest = onDismissRequest,
            onAddFeedClick = onAddFeedClick,
            onImportExportClick = onImportExportClick,
            onAccountsClick = onAccountsClick,
            onFeedSuggestionsClick = onFeedSuggestionsClick,
        )
    }
}

@PreviewPhone
@Composable
private fun NoFeedsBottomSheetPreview() {
    FeedFlowTheme {
        NoFeedsBottomSheet(
            onAddFeedClick = {},
            onImportExportClick = {},
            onDismissRequest = {},
            onAccountsClick = {},
        )
    }
}
