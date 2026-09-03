package com.prof18.feedflow.feedsync.googledrive

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
import java.io.IOException

internal class StartupFallbackVerificationCodeReceiver(
    private val primaryReceiver: VerificationCodeReceiver,
    private val fallbackReceiverFactory: () -> VerificationCodeReceiver,
    private val onFallback: (IOException) -> Unit = {},
) : VerificationCodeReceiver {

    private val lock = Any()
    private var activeReceiver = primaryReceiver
    private var redirectUriResolved = false

    override fun getRedirectUri(): String = synchronized(lock) {
        if (redirectUriResolved) {
            throw IOException("Loopback receiver is already running")
        }

        try {
            primaryReceiver.redirectUri.also { redirectUriResolved = true }
        } catch (primaryFailure: IOException) {
            stopPrimaryAfterStartupFailure(primaryFailure)
            onFallback(primaryFailure)

            val fallbackReceiver = fallbackReceiverFactory()
            activeReceiver = fallbackReceiver
            try {
                fallbackReceiver.redirectUri.also { redirectUriResolved = true }
            } catch (fallbackFailure: IOException) {
                fallbackFailure.addSuppressed(primaryFailure)
                throw fallbackFailure
            }
        }
    }

    override fun waitForCode(): String {
        val receiver = synchronized(lock) {
            if (!redirectUriResolved) {
                throw IOException("Loopback receiver has not been started")
            }
            activeReceiver
        }
        return receiver.waitForCode()
    }

    override fun stop() {
        val receiver = synchronized(lock) { activeReceiver }
        receiver.stop()
    }

    private fun stopPrimaryAfterStartupFailure(primaryFailure: IOException) {
        try {
            primaryReceiver.stop()
        } catch (stopFailure: IOException) {
            primaryFailure.addSuppressed(stopFailure)
        }
    }
}
