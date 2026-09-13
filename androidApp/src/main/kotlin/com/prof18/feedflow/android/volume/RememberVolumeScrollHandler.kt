package com.prof18.feedflow.android.volume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun RememberVolumeScrollHandler(handler: VolumeScrollHandler) {
    DisposableEffect(handler) {
        VolumeScrollController.push(handler)
        onDispose {
            VolumeScrollController.remove(handler)
        }
    }
}
