package com.prof18.feedflow.home.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.prof18.feedflow.ui.home.components.NoFeedsInfoContent
import md_theme_dark_background
import md_theme_light_background

@OptIn(ExperimentalMaterial3Api::class)
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
