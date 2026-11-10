package com.prof18.feedflow.desktop.testing

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import java.io.File

/**
 * Debug-only controls for the desktop E2E suite (e2e/desktop/README.md). Every value comes from a JVM
 * system property and is ignored in release builds.
 */
internal object DesktopE2eHooks {
    private val seedParts = 2..3

    @Volatile
    private var config: DesktopE2eConfig? = null

    fun install(appEnvironment: AppEnvironment, read: (String) -> String? = System::getProperty) {
        fun value(key: String) = DesktopE2eProperties.debugValue(appEnvironment, key, read)
        config = if (appEnvironment.isDebug()) {
            DesktopE2eConfig(
                seed = value(DesktopE2eProperties.SEED),
                seedAudioUrl = value(DesktopE2eProperties.SEED_AUDIO_URL),
                fileDialogResponse = value(DesktopE2eProperties.FILE_DIALOG_RESPONSE)?.let(::File),
                openedUriLog = value(DesktopE2eProperties.OPENED_URI_LOG)?.let(::File),
            )
        } else {
            null
        }
    }

    fun seedSpec(): DesktopE2eSeedSpec? {
        val current = config ?: return null
        val seed = current.seed ?: return null
        val parts = seed.split(':')
        require(parts.size in seedParts) {
            "${DesktopE2eProperties.SEED} must be <action>:<profile>[:<account>], was '$seed'"
        }
        return DesktopE2eSeedSpec(
            action = parts[0],
            profile = parts[1],
            account = parts.getOrNull(2),
            audioUrl = current.seedAudioUrl,
        )
    }

    fun fileDialogScript(): FileDialogScript {
        val response = config?.fileDialogResponse ?: return FileDialogScript.NotScripted
        val firstLine = response.takeIf { it.isFile }?.useLines { it.firstOrNull() }?.trim()
        return if (firstLine.isNullOrEmpty()) FileDialogScript.Cancel else FileDialogScript.Choose(File(firstLine))
    }

    fun recordOpenedUri(uri: String): Boolean {
        val log = config?.openedUriLog ?: return false
        synchronized(this) { log.appendText("$uri\n") }
        return true
    }

    internal fun reset() {
        config = null
    }
}

private data class DesktopE2eConfig(
    val seed: String?,
    val seedAudioUrl: String?,
    val fileDialogResponse: File?,
    val openedUriLog: File?,
)

internal data class DesktopE2eSeedSpec(
    val action: String,
    val profile: String,
    val account: String?,
    val audioUrl: String?,
)

internal sealed interface FileDialogScript {
    data object NotScripted : FileDialogScript
    data object Cancel : FileDialogScript
    data class Choose(val file: File) : FileDialogScript
}
