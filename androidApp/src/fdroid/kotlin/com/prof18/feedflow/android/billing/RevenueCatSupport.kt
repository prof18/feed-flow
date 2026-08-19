package com.prof18.feedflow.android.billing

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object RevenueCatSupport {
    const val isAvailable = false

    @Suppress("UnusedParameter")
    fun configure(
        context: Context,
        apiKey: String,
        isDebug: Boolean,
    ) = Unit
}

@Suppress("UnusedParameter")
@Composable
fun RevenueCatSupportPaywall(
    onDismiss: () -> Unit,
    onPurchaseCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
