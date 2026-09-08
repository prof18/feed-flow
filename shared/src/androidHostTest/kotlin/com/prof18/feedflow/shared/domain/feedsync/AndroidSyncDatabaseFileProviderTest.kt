package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidSyncDatabaseFileProviderTest : KoinTestBase() {
    private val context: Context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `debug provider resolves Android database and remote names`() {
        assertProviderNames(
            appEnvironment = AppEnvironment.Debug,
            databaseName = SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG,
        )
    }

    @Test
    fun `release provider resolves Android database and remote names`() {
        assertProviderNames(
            appEnvironment = AppEnvironment.Release,
            databaseName = SyncedDatabaseHelper.SYNC_DATABASE_NAME_PROD,
        )
    }

    private fun assertProviderNames(appEnvironment: AppEnvironment, databaseName: String) {
        val provider = AndroidSyncDatabaseFileProvider(context, appEnvironment)

        assertEquals(context.getDatabasePath(databaseName), provider.databaseFile)
        assertEquals(databaseName, provider.databaseFile.name)
        assertEquals("$databaseName.db", provider.remoteFileName)
    }
}
