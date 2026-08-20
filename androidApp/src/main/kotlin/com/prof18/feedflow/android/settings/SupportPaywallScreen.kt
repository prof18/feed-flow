package com.prof18.feedflow.android.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.android.billing.RevenueCatSupportPaywall
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun SupportPaywallScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    var isShowingPurchaseThankYou by rememberSaveable { mutableStateOf(false) }
    val paywallBackgroundColor = if (isSystemInDarkTheme()) {
        PaywallDarkBackgroundColor
    } else {
        PaywallLightBackgroundColor
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(strings.supportPaywallTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = paywallBackgroundColor,
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            RevenueCatSupportPaywall(
                onDismiss = {
                    if (!isShowingPurchaseThankYou) {
                        onNavigateBack()
                    }
                },
                onPurchaseCompleted = { isShowingPurchaseThankYou = true },
            )
        }
    }

    if (isShowingPurchaseThankYou) {
        SupportPurchaseThankYouDialog(
            onDismiss = {
                isShowingPurchaseThankYou = false
                onNavigateBack()
            },
        )
    }
}

private val PaywallDarkBackgroundColor = Color(0xFF1A1B1F)
private val PaywallLightBackgroundColor = Color(0xFFFAFAFA)
