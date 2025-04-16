package com.prof18.feedflow.android.settings.components

import FeedFlowTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun SyncPeriodDialog(
    currentPeriod: SyncPeriod,
    onPeriodSelected: (SyncPeriod) -> Unit,
    showNeverSync: Boolean = true,
    description: String = LocalFeedFlowStrings.current.settingsSyncPeriodDesc,
    dismissDialog: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Dialog(onDismissRequest = dismissDialog) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.regular),
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.regular),
            )

            LazyColumn {
                if (showNeverSync) {
                    item {
                        PeriodOption(
                            text = strings.settingsSyncPeriodNever,
                            selected = currentPeriod == SyncPeriod.NEVER,
                            onClick = {
                                onPeriodSelected(SyncPeriod.NEVER)
                                dismissDialog()
                            },
                        )
                    }
                }
                item {
                    PeriodOption(
                        text = strings.settingsSyncPeriodFifteenMinutes,
                        selected = currentPeriod == SyncPeriod.FIFTEEN_MINUTES,
                        onClick = {
                            onPeriodSelected(SyncPeriod.FIFTEEN_MINUTES)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsSyncPeriodThirtyMinutes,
                        selected = currentPeriod == SyncPeriod.THIRTY_MINUTES,
                        onClick = {
                            onPeriodSelected(SyncPeriod.THIRTY_MINUTES)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsSyncPeriodOneHour,
                        selected = currentPeriod == SyncPeriod.ONE_HOUR,
                        onClick = {
                            onPeriodSelected(SyncPeriod.ONE_HOUR)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsSyncPeriodTwoHours,
                        selected = currentPeriod == SyncPeriod.TWO_HOURS,
                        onClick = {
                            onPeriodSelected(SyncPeriod.TWO_HOURS)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsSyncPeriodSixHours,
                        selected = currentPeriod == SyncPeriod.SIX_HOURS,
                        onClick = {
                            onPeriodSelected(SyncPeriod.SIX_HOURS)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsSyncPeriodTwelveHours,
                        selected = currentPeriod == SyncPeriod.TWELVE_HOURS,
                        onClick = {
                            onPeriodSelected(SyncPeriod.TWELVE_HOURS)
                            dismissDialog()
                        },
                    )
                }
                item {
                    PeriodOption(
                        text = strings.settingsSyncPeriodOneDay,
                        selected = currentPeriod == SyncPeriod.ONE_DAY,
                        onClick = {
                            onPeriodSelected(SyncPeriod.ONE_DAY)
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
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@PreviewPhone
@Composable
private fun SyncPeriodDialogPreview() {
    FeedFlowTheme {
        SyncPeriodDialog(
            currentPeriod = SyncPeriod.ONE_HOUR,
            onPeriodSelected = {},
            dismissDialog = {},
        )
    }
}
