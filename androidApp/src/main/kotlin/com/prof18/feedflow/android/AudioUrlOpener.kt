package com.prof18.feedflow.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.prof18.feedflow.core.model.AudioEnclosure

internal class AudioUrlOpener(
    private val launcher: (Intent) -> Unit,
) {
    fun open(url: String): Boolean {
        val validatedUrl = AudioEnclosure.validatedUrl(url) ?: return false
        val uri = Uri.parse(validatedUrl).normalizeScheme()
        val audioIntent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "audio/*")
        return tryLaunch(audioIntent) || tryLaunch(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun tryLaunch(intent: Intent): Boolean = try {
        launcher(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
