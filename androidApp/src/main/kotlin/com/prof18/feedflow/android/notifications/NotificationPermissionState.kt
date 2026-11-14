package com.prof18.feedflow.android.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Stable
internal class NotificationPermissionState(
    val isGranted: Boolean,
    val request: () -> Unit,
) {
    fun requestIfTurnedOn(enabled: Boolean) {
        if (enabled && !isGranted) request()
    }
}

@Composable
internal fun rememberNotificationPermission(): NotificationPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var isGranted by remember { mutableStateOf(areNotificationsEnabled(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isGranted = areNotificationsEnabled(context)
        if (!granted && !shouldShowPermissionDialog(activity)) {
            openAppNotificationSettings(context)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isGranted = areNotificationsEnabled(context)
    }
    return NotificationPermissionState(isGranted = isGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasRuntimeNotificationPermission(context)
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppNotificationSettings(context)
        }
    }
}

private fun areNotificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        hasRuntimeNotificationPermission(context)

private fun hasRuntimeNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

private fun shouldShowPermissionDialog(activity: Activity?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ?: false
    } else {
        false
    }
}

private fun openAppNotificationSettings(context: Context) {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        context.startActivity(this)
    }
}
