package com.prof18.feedflow.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview


// TODO: add bigger device preview?
@Preview(
    name = "Light theme",
    group = "themes",
)
@Preview(
    name = "Dark theme",
    group = "themes",
    uiMode = UI_MODE_NIGHT_YES
)
annotation class FeedFlowPreview