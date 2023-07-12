package com.prof18.feedflow.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.domain.model.Browser

@Composable
internal fun BrowserSelectionDialog(
    browserList: List<Browser>,
    onBrowserSelected: (Browser) -> Unit,
    dismissDialog: () -> Unit,
) {
    Dialog(onDismissRequest = dismissDialog) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
        ) {
            browserList.forEach { browser ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = browser.isFavourite,
                            onClick = {
                                onBrowserSelected(browser)
                                dismissDialog()
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = browser.isFavourite,
                        onClick = {
                            onBrowserSelected(browser)
                            dismissDialog()
                        }
                    )
                    Text(
                        text = browser.name,
                    )
                }
            }
        }
    }
}
