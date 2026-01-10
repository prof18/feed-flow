package com.prof18.feedflow.android.util

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberSystemReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    var isReducedMotionEnabled by remember {
        mutableStateOf(!ValueAnimator.areAnimatorsEnabled())
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isReducedMotionEnabled = !ValueAnimator.areAnimatorsEnabled()
            }
        }

        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    return isReducedMotionEnabled
}
