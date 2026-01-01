package com.prof18.feedflow.shared.ui.feed

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT
import com.prof18.feedflow.core.model.LinkOpeningPreference.INTERNAL_BROWSER
import com.prof18.feedflow.core.model.LinkOpeningPreference.PREFERRED_BROWSER
import com.prof18.feedflow.core.model.LinkOpeningPreference.READER_MODE
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun LinkOpeningPreferenceSelector(
    currentPreference: LinkOpeningPreference,
    onPreferenceSelected: (LinkOpeningPreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = when (currentPreference) {
                DEFAULT -> LocalFeedFlowStrings.current.linkOpeningPreferenceDefault
                READER_MODE -> LocalFeedFlowStrings.current.linkOpeningPreferenceReaderMode
                INTERNAL_BROWSER -> LocalFeedFlowStrings.current.linkOpeningPreferenceInternalBrowser
                PREFERRED_BROWSER -> LocalFeedFlowStrings.current.linkOpeningPreferencePreferredBrowser
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(LocalFeedFlowStrings.current.linkOpeningPreference) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LinkOpeningPreference.entries.forEach { preference ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (preference) {
                                DEFAULT -> LocalFeedFlowStrings.current.linkOpeningPreferenceDefault
                                READER_MODE -> LocalFeedFlowStrings.current.linkOpeningPreferenceReaderMode
                                INTERNAL_BROWSER -> LocalFeedFlowStrings.current.linkOpeningPreferenceInternalBrowser
                                PREFERRED_BROWSER -> LocalFeedFlowStrings.current.linkOpeningPreferencePreferredBrowser
                            },
                        )
                    },
                    onClick = {
                        onPreferenceSelected(preference)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
