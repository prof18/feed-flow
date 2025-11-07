package com.prof18.feedflow.android.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AutoDeletePeriodDialog(
    currentPeriod: AutoDeletePeriod,
    onPeriodSelected: (AutoDeletePeriod) -> Unit,
    dismissDialog: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Dialog(onDismissRequest = dismissDialog) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.regular),
        ) {
            Text(
                text = strings.settingsAutoDelete,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .padding(bottom = Spacing.regular),
            )

            Text(
                text = strings.settingsAutoDeleteDesc,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.regular),
            )

            LazyColumn {
                item {
                    PeriodOption(
                        text = strings.settingsAutoDeletePeriodDisabled,
                        selected = currentPeriod == AutoDeletePeriod.DISABLED,
                        onClick = {
                            onPeriodSelected(AutoDeletePeriod.DISABLED)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsAutoDeletePeriodOneDay,
                        selected = currentPeriod == AutoDeletePeriod.ONE_DAY,
                        onClick = {
                            onPeriodSelected(AutoDeletePeriod.ONE_DAY)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsAutoDeletePeriodOneWeek,
                        selected = currentPeriod == AutoDeletePeriod.ONE_WEEK,
                        onClick = {
                            onPeriodSelected(AutoDeletePeriod.ONE_WEEK)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsAutoDeletePeriodTwoWeeks,
                        selected = currentPeriod == AutoDeletePeriod.TWO_WEEKS,
                        onClick = {
                            onPeriodSelected(AutoDeletePeriod.TWO_WEEKS)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsAutoDeletePeriodOneMonth,
                        selected = currentPeriod == AutoDeletePeriod.ONE_MONTH,
                        onClick = {
                            onPeriodSelected(AutoDeletePeriod.ONE_MONTH)
                            dismissDialog()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.small),
        )
    }
}

@PreviewPhone
@Composable
private fun AutoDeletePeriodDialogPreview() {
    FeedFlowTheme {
        AutoDeletePeriodDialog(
            currentPeriod = AutoDeletePeriod.DISABLED,
            onPeriodSelected = {},
            dismissDialog = {},
        )
    }
}
