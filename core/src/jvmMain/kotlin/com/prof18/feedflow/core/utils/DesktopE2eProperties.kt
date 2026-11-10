package com.prof18.feedflow.core.utils

object DesktopE2eProperties {
    const val DATA_DIR = "feedflow.e2e.dataDir"
    const val PREFS_NODE = "feedflow.e2e.prefsNode"
    const val SEED = "feedflow.e2e.seed"
    const val SEED_AUDIO_URL = "feedflow.e2e.seedAudioUrl"
    const val FILE_DIALOG_RESPONSE = "feedflow.e2e.fileDialogResponse"
    const val OPENED_URI_LOG = "feedflow.e2e.openedUriLog"

    fun debugValue(
        appEnvironment: AppEnvironment,
        key: String,
        read: (String) -> String? = System::getProperty,
    ): String? = if (appEnvironment.isDebug()) read(key)?.takeIf { it.isNotBlank() } else null
}
