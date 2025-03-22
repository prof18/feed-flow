package com.prof18.feedflow.shared.data

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import platform.Foundation.NSBundle

class KeychainSettingsMigration(
    private val oldSettings: Settings,
    private val newSettings: Settings,
) {
    fun performMigrationIfNeeded() {
        val isMigrationDone = newSettings
            .getBoolean(SettingsFields.IS_KEYCHAIN_MIGRATION_DONE.name, false)

        if (isMigrationDone) {
            Logger.d { "Keychain Migration already done, skipping" }
            return
        }

        val mainBundle = NSBundle.mainBundle
        val version = mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "Unknown"

        val lastVersionNumber = version.split(".")
            .lastOrNull()
            ?.toIntOrNull() ?: 0

        Logger.d { "Last version number: $lastVersionNumber" }

        @Suppress("MagicNumber")
        if (lastVersionNumber >= 85) {
            Logger.d { "Version is >= 85, performing migration..." }
            migrate()
        } else {
            Logger.d { "Version is < 85, skipping migration" }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun migrate() {
        oldSettings.getStringOrNull(SettingsFields.FAVOURITE_BROWSER_ID.name)?.let {
            newSettings[SettingsFields.FAVOURITE_BROWSER_ID.name] = it
        }.also {
            oldSettings.remove(SettingsFields.FAVOURITE_BROWSER_ID.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name)?.let {
            newSettings[SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name] = it
        }.also {
            oldSettings.remove(SettingsFields.MARK_FEED_AS_READ_WHEN_SCROLLING.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name)?.let {
            newSettings[SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name] = it
        }.also {
            oldSettings.remove(SettingsFields.SHOW_READ_ARTICLES_TIMELINE.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.USE_READER_MODE.name)?.let {
            newSettings[SettingsFields.USE_READER_MODE.name] = it
        }.also {
            oldSettings.remove(SettingsFields.USE_READER_MODE.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name)?.let {
            newSettings[SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name] = it
        }.also {
            oldSettings.remove(SettingsFields.IS_SYNC_UPLOAD_REQUIRED.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.REMOVE_TITLE_FROM_DESCRIPTION.name)?.let {
            newSettings[SettingsFields.REMOVE_TITLE_FROM_DESCRIPTION.name] = it
        }.also {
            oldSettings.remove(SettingsFields.REMOVE_TITLE_FROM_DESCRIPTION.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.HIDE_DESCRIPTION.name)?.let {
            newSettings[SettingsFields.HIDE_DESCRIPTION.name] = it
        }.also {
            oldSettings.remove(SettingsFields.HIDE_DESCRIPTION.name)
        }

        oldSettings.getIntOrNull(SettingsFields.READER_MODE_FONT_SIZE.name)?.let {
            newSettings[SettingsFields.READER_MODE_FONT_SIZE.name] = it
        }.also {
            oldSettings.remove(SettingsFields.READER_MODE_FONT_SIZE.name)
        }

        oldSettings.getIntOrNull(SettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name)?.let {
            newSettings[SettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name] = it
        }.also {
            oldSettings.remove(SettingsFields.FEED_LIST_FONT_SCALE_FACTOR.name)
        }

        oldSettings.getStringOrNull(SettingsFields.AUTO_DELETE_PERIOD.name)?.let {
            newSettings[SettingsFields.AUTO_DELETE_PERIOD.name] = it
        }.also {
            oldSettings.remove(SettingsFields.AUTO_DELETE_PERIOD.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.HIDE_IMAGES.name)?.let {
            newSettings[SettingsFields.HIDE_IMAGES.name] = it
        }.also {
            oldSettings.remove(SettingsFields.HIDE_IMAGES.name)
        }

        oldSettings.getBooleanOrNull(SettingsFields.CRASH_REPORTING_ENABLED.name)?.let {
            newSettings[SettingsFields.CRASH_REPORTING_ENABLED.name] = it
        }.also {
            oldSettings.remove(SettingsFields.CRASH_REPORTING_ENABLED.name)
        }

        oldSettings.getStringOrNull(SettingsFields.SYNC_PERIOD.name)?.let {
            newSettings[SettingsFields.SYNC_PERIOD.name] = it
        }.also {
            oldSettings.remove(SettingsFields.SYNC_PERIOD.name)
        }

        oldSettings.getLongOrNull(SettingsFields.FIRST_INSTALLATION_DATE.name)?.let {
            newSettings[SettingsFields.FIRST_INSTALLATION_DATE.name] = it
        }.also {
            oldSettings.remove(SettingsFields.FIRST_INSTALLATION_DATE.name)
        }

        oldSettings.getIntOrNull(SettingsFields.REVIEW_REQUEST_COUNT.name)?.let {
            newSettings[SettingsFields.REVIEW_REQUEST_COUNT.name] = it
        }.also {
            oldSettings.remove(SettingsFields.REVIEW_REQUEST_COUNT.name)
        }

        oldSettings.getLongOrNull(SettingsFields.LAST_REVIEW_REQUEST_DATE.name)?.let {
            newSettings[SettingsFields.LAST_REVIEW_REQUEST_DATE.name] = it
        }.also {
            oldSettings.remove(SettingsFields.LAST_REVIEW_REQUEST_DATE.name)
        }

        oldSettings.getStringOrNull(SettingsFields.LAST_REVIEW_VERSION.name)?.let {
            newSettings[SettingsFields.LAST_REVIEW_VERSION.name] = it
        }.also {
            oldSettings.remove(SettingsFields.LAST_REVIEW_VERSION.name)
        }

        oldSettings.getStringOrNull("DROPBOX_DATA")?.let {
            newSettings["DROPBOX_DATA"] = it
        }.also {
            oldSettings.remove("DROPBOX_DATA")
        }

        oldSettings.getLongOrNull("LAST_UPLOAD_TIMESTAMP")?.let {
            newSettings["LAST_UPLOAD_TIMESTAMP"] = it
        }.also {
            oldSettings.remove("LAST_UPLOAD_TIMESTAMP")
        }

        oldSettings.getLongOrNull("LAST_DOWNLOAD_TIMESTAMP")?.let {
            newSettings["LAST_DOWNLOAD_TIMESTAMP"] = it
        }.also {
            oldSettings.remove("LAST_DOWNLOAD_TIMESTAMP")
        }

        oldSettings.getBooleanOrNull("USE_ICLOUD")?.let {
            newSettings["USE_ICLOUD"] = it
        }.also {
            oldSettings.remove("USE_ICLOUD")
        }

        oldSettings.getLongOrNull("ICLOUD_LAST_UPLOAD_TIMESTAMP")?.let {
            newSettings["ICLOUD_LAST_UPLOAD_TIMESTAMP"] = it
        }.also {
            oldSettings.remove("ICLOUD_LAST_UPLOAD_TIMESTAMP")
        }

        oldSettings.getLongOrNull("ICLOUD_LAST_DOWNLOAD_TIMESTAMP")?.let {
            newSettings["ICLOUD_LAST_DOWNLOAD_TIMESTAMP"] = it
        }.also {
            oldSettings.remove("ICLOUD_LAST_DOWNLOAD_TIMESTAMP")
        }

        oldSettings.getStringOrNull("SYNC_PWD")?.let {
            newSettings["SYNC_PWD"] = it
        }.also {
            oldSettings.remove("SYNC_PWD")
        }

        oldSettings.getStringOrNull("SYNC_USERNAME")?.let {
            newSettings["SYNC_USERNAME"] = it
        }.also {
            oldSettings.remove("SYNC_USERNAME")
        }

        oldSettings.getStringOrNull("SYNC_URL")?.let {
            newSettings["SYNC_URL"] = it
        }.also {
            oldSettings.remove("SYNC_URL")
        }

        oldSettings.getLongOrNull("LAST_SYNC_DATE")?.let {
            newSettings["LAST_SYNC_DATE"] = it
        }.also {
            oldSettings.remove("LAST_SYNC_DATE")
        }

        newSettings[SettingsFields.IS_KEYCHAIN_MIGRATION_DONE.name] = true

        Logger.d { "Migration done" }
    }
}
