package com.prof18.feedflow.android.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.presentation.model.HomeViewMenuState
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeViewOptionsBottomSheet(
    state: HomeViewMenuState,
    onFeedOrderChange: (FeedOrder) -> Unit,
    onShowReadArticlesTimelineChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val strings = LocalFeedFlowStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular)
                .padding(bottom = Spacing.regular),
        ) {
            Text(
                text = strings.settingsFeedOrderTitle,
                modifier = Modifier.padding(bottom = Spacing.small),
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.feedOrder == FeedOrder.NEWEST_FIRST,
                    onClick = { onFeedOrderChange(FeedOrder.NEWEST_FIRST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(strings.settingsFeedOrderNewestFirst)
                }

                SegmentedButton(
                    selected = state.feedOrder == FeedOrder.OLDEST_FIRST,
                    onClick = { onFeedOrderChange(FeedOrder.OLDEST_FIRST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(strings.settingsFeedOrderOldestFirst)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.regular))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.settingsToggleShowReadArticles,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.showReadArticlesTimeline,
                    onCheckedChange = onShowReadArticlesTimelineChange,
                )
            }
        }
    }
}
