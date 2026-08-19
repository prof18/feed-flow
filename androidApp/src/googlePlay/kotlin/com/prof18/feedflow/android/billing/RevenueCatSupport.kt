package com.prof18.feedflow.android.billing

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

object RevenueCatSupport {
    var isAvailable: Boolean = false
        private set

    fun configure(
        context: Context,
        apiKey: String,
        isDebug: Boolean,
    ) {
        if (apiKey.isBlank()) {
            return
        }

        if (isDebug) {
            Purchases.logLevel = LogLevel.DEBUG
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(context, apiKey).build(),
        )
        isAvailable = true
    }
}

@Composable
fun RevenueCatSupportPaywall(
    onDismiss: () -> Unit,
    onPurchaseCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var paywallState by remember { mutableStateOf<SupportPaywallState>(SupportPaywallState.Loading) }

    LaunchedEffect(Unit) {
        Purchases.sharedInstance.getOfferings(
            object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                    paywallState = offerings.current
                        ?.let(SupportPaywallState::Ready)
                        ?: SupportPaywallState.Fallback
                }

                override fun onError(error: PurchasesError) {
                    paywallState = SupportPaywallState.Fallback
                }
            },
        )
    }

    Box(modifier = modifier) {
        when (val currentState = paywallState) {
            SupportPaywallState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            is SupportPaywallState.Ready -> {
                RevenueCatPaywall(
                    offering = currentState.offering,
                    onDismiss = onDismiss,
                    onPurchaseCompleted = onPurchaseCompleted,
                )
            }

            SupportPaywallState.Fallback -> {
                RevenueCatPaywall(
                    offering = null,
                    onDismiss = onDismiss,
                    onPurchaseCompleted = onPurchaseCompleted,
                )
            }
        }
    }
}

@Composable
private fun RevenueCatPaywall(
    offering: Offering?,
    onDismiss: () -> Unit,
    onPurchaseCompleted: () -> Unit,
) {
    val listener = remember(onPurchaseCompleted) {
        object : PaywallListener {
            override fun onPurchaseCompleted(
                customerInfo: com.revenuecat.purchases.CustomerInfo,
                storeTransaction: com.revenuecat.purchases.models.StoreTransaction,
            ) {
                onPurchaseCompleted()
            }
        }
    }
    val options = remember(onDismiss, listener, offering) {
        PaywallOptions.Builder(onDismiss)
            .setShouldDisplayDismissButton(false)
            .setListener(listener)
            .apply {
                offering?.let(::setOffering)
            }
            .build()
    }
    Paywall(options = options)
}

private sealed interface SupportPaywallState {
    data object Loading : SupportPaywallState

    data class Ready(
        val offering: Offering,
    ) : SupportPaywallState

    data object Fallback : SupportPaywallState
}
