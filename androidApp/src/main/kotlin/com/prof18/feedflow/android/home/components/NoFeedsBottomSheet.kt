package com.prof18.feedflow.android.home.components

import FeedFlowTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.prof18.feedflow.shared.ui.home.components.NoFeedsInfoContent
import com.prof18.feedflow.shared.ui.preview.FeedFlowPhonePreview
import md_theme_dark_background
import md_theme_light_background

@Composable
internal fun NoFeedsBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
    onAddFeedClick: () -> Unit,
    onImportExportClick: () -> Unit,
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
            onDismissRequest = onDismissRequest,
            onAddFeedClick = onAddFeedClick,
            onImportExportClick = onImportExportClick,
        )
    }
}

@FeedFlowPhonePreview
@Composable
private fun NoFeedsBottomSheetPreview() {
    FeedFlowTheme {
        NoFeedsBottomSheet(
            onAddFeedClick = {},
            onImportExportClick = {},
            onDismissRequest = {},
        )
    }
}