package com.prof18.feedflow.shared.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Light theme",
    group = "themes",
    backgroundColor = 0xFFFFFFFF,
    showBackground = true,
)
@Preview(
    name = "ZDark theme",
    group = "themes",
    uiMode = UI_MODE_NIGHT_YES,
)
annotation class PreviewPhone

@Preview(
    name = "Light theme",
    group = "themes",
    backgroundColor = 0xFFFFFFFF,
    showBackground = true,
    device = "spec:width=673dp,height=841dp",
)
@Preview(
    name = "ZDark theme",
    group = "themes",
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:width=673dp,height=841dp",
)
annotation class PreviewFoldable

@Preview(
    name = "Light theme",
    group = "themes",
    backgroundColor = 0xFFFFFFFF,
    showBackground = true,
    device = "id:pixel_tablet",
)
@Preview(
    name = "ZDark theme",
    group = "themes",
    uiMode = UI_MODE_NIGHT_YES,
    device = "id:pixel_tablet",
)
annotation class PreviewTablet
