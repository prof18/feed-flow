package com.prof18.feedflow.shared.ui.feed

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
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
import com.prof18.feedflow.core.model.LinkOpeningPreference.*
import com.prof18.feedflow.shared.ui.style.Spacing
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
                DEFAULT -> "Use default settings"
                READER_MODE -> "Always use reader mode"
                INTERNAL_BROWSER -> "Always use internal browser"
                PREFERRED_BROWSER -> "Always use preferred browser"
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("Link opening preference") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LinkOpeningPreference.values().forEach { preference ->
                DropdownMenuItem(
                    text = { Text(when (preference) {
                        DEFAULT -> "Use default settings"
                        READER_MODE -> "Always use reader mode"
                        INTERNAL_BROWSER -> "Always use internal browser"
                        PREFERRED_BROWSER -> "Always use preferred browser"
                    }) },
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
