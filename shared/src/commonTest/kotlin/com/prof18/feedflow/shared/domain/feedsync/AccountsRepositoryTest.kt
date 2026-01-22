package com.prof18.feedflow.shared.domain.feedsync

import app.cash.turbine.test
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.CurrentOS
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountsRepositoryTest : KoinTestBase() {

    private val accountsRepository: AccountsRepository by inject()
    private val dropboxSettings: DropboxSettings by inject()
    private val googleDriveSettings: GoogleDriveSettings by inject()
    private val icloudSettings: ICloudSettings by inject()
    private val networkSettings: NetworkSettings by inject()

    private var currentOS: CurrentOS = CurrentOS.Android
    private var isDropboxSyncEnabled = true
    private var isGoogleDriveSyncEnabled = true
    private var isIcloudSyncEnabled = true

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<CurrentOS> { currentOS }
        single {
            AppConfig(
                appEnvironment = AppEnvironment.Debug,
                isLoggingEnabled = true,
                isDropboxSyncEnabled = isDropboxSyncEnabled,
                isGoogleDriveSyncEnabled = isGoogleDriveSyncEnabled,
                isIcloudSyncEnabled = isIcloudSyncEnabled,
                appVersion = "1.0.0",
                platformName = "Test",
                platformVersion = "1.0.0",
            )
        }
        single {
            AccountsRepository(
                currentOS = get(),
                dropboxSettings = get(),
                googleDriveSettings = get(),
                icloudSettings = get(),
                appConfig = get(),
                gReaderRepository = get(),
                networkSettings = get(),
                feedbinRepository = get(),
            )
        }
    }

    @Test
    fun `getValidAccounts returns correct accounts for Android`() = runTest {
        currentOS = CurrentOS.Android

        val expectedAccounts = listOf(
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )

        val validAccounts = accountsRepository.getValidAccounts()
        assertEquals(expectedAccounts.sorted(), validAccounts.sorted())
    }

    @Test
    fun `getValidAccounts returns correct accounts for iOS`() = runTest {
        currentOS = CurrentOS.Ios

        val expectedAccounts = listOf(
            SyncAccounts.ICLOUD,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )

        val validAccounts = accountsRepository.getValidAccounts()
        assertEquals(expectedAccounts.sorted(), validAccounts.sorted())
    }

    @Test
    fun `getValidAccounts returns correct accounts for macOS`() = runTest {
        currentOS = CurrentOS.Desktop.Mac

        val expectedAccounts = listOf(
            SyncAccounts.ICLOUD,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )

        val validAccounts = accountsRepository.getValidAccounts()
        assertEquals(expectedAccounts.sorted(), validAccounts.sorted())
    }

    @Test
    fun `getValidAccounts returns correct accounts for Windows`() = runTest {
        currentOS = CurrentOS.Desktop.Windows

        val expectedAccounts = listOf(
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )

        val validAccounts = accountsRepository.getValidAccounts()
        assertEquals(expectedAccounts.sorted(), validAccounts.sorted())
    }

    @Test
    fun `getValidAccounts returns correct accounts for Linux`() = runTest {
        currentOS = CurrentOS.Desktop.Linux

        val expectedAccounts = listOf(
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )

        val validAccounts = accountsRepository.getValidAccounts()
        assertEquals(expectedAccounts.sorted(), validAccounts.sorted())
    }

    @Test
    fun `getValidAccounts respects AppConfig flags`() = runTest {
        currentOS = CurrentOS.Ios
        isDropboxSyncEnabled = false
        isGoogleDriveSyncEnabled = false
        isIcloudSyncEnabled = false

        val expectedAccounts = listOf(
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.FEEDBIN,
            SyncAccounts.BAZQUX,
        )

        val validAccounts = accountsRepository.getValidAccounts()
        assertEquals(expectedAccounts.sorted(), validAccounts.sorted())
    }

    @Test
    fun `setDropboxAccount updates state and clears others`() = runTest {
        googleDriveSettings.setGoogleDriveLinked(true)
        icloudSettings.setUseICloud(true)
        networkSettings.setSyncUrl("http://localhost")

        accountsRepository.setDropboxAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.DROPBOX, awaitItem())
        }
        assertFalse(googleDriveSettings.isGoogleDriveLinked())
        assertFalse(icloudSettings.getUseICloud())
        assertEquals("", networkSettings.getSyncUrl())
    }

    @Test
    fun `setGoogleDriveAccount updates state and clears others`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        icloudSettings.setUseICloud(true)
        networkSettings.setSyncUrl("http://localhost")

        accountsRepository.setGoogleDriveAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.GOOGLE_DRIVE, awaitItem())
        }
        assertEquals(null, dropboxSettings.getDropboxData())
        assertFalse(icloudSettings.getUseICloud())
        assertEquals("", networkSettings.getSyncUrl())
    }

    @Test
    fun `setICloudAccount updates state and clears others`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        googleDriveSettings.setGoogleDriveLinked(true)
        networkSettings.setSyncUrl("http://localhost")

        accountsRepository.setICloudAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.ICLOUD, awaitItem())
        }
        assertEquals(null, dropboxSettings.getDropboxData())
        assertFalse(googleDriveSettings.isGoogleDriveLinked())
        assertEquals("", networkSettings.getSyncUrl())
    }

    @Test
    fun `setFreshRssAccount updates state and clears others`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        googleDriveSettings.setGoogleDriveLinked(true)
        icloudSettings.setUseICloud(true)

        accountsRepository.setFreshRssAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.FRESH_RSS, awaitItem())
        }
        assertEquals(null, dropboxSettings.getDropboxData())
        assertFalse(googleDriveSettings.isGoogleDriveLinked())
        assertFalse(icloudSettings.getUseICloud())
        assertEquals(SyncAccounts.FRESH_RSS, networkSettings.getSyncAccountType())
    }

    @Test
    fun `setMinifluxAccount updates state and clears others`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        googleDriveSettings.setGoogleDriveLinked(true)
        icloudSettings.setUseICloud(true)

        accountsRepository.setMinifluxAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.MINIFLUX, awaitItem())
        }
        assertEquals(null, dropboxSettings.getDropboxData())
        assertFalse(googleDriveSettings.isGoogleDriveLinked())
        assertFalse(icloudSettings.getUseICloud())
        assertEquals(SyncAccounts.MINIFLUX, networkSettings.getSyncAccountType())
    }

    @Test
    fun `setBazquxAccount updates state and clears others`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        googleDriveSettings.setGoogleDriveLinked(true)
        icloudSettings.setUseICloud(true)

        accountsRepository.setBazquxAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.BAZQUX, awaitItem())
        }
        assertEquals(null, dropboxSettings.getDropboxData())
        assertFalse(googleDriveSettings.isGoogleDriveLinked())
        assertFalse(icloudSettings.getUseICloud())
        assertEquals(SyncAccounts.BAZQUX, networkSettings.getSyncAccountType())
    }

    @Test
    fun `setFeedbinAccount updates state and clears others`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        googleDriveSettings.setGoogleDriveLinked(true)
        icloudSettings.setUseICloud(true)

        accountsRepository.setFeedbinAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.FEEDBIN, awaitItem())
        }
        assertEquals(null, dropboxSettings.getDropboxData())
        assertFalse(googleDriveSettings.isGoogleDriveLinked())
        assertFalse(icloudSettings.getUseICloud())
        assertEquals(SyncAccounts.FEEDBIN, networkSettings.getSyncAccountType())
    }

    @Test
    fun `clearAccount updates state to LOCAL`() = runTest {
        accountsRepository.setDropboxAccount()
        accountsRepository.clearAccount()

        accountsRepository.currentAccountState.test {
            assertEquals(SyncAccounts.LOCAL, awaitItem())
        }
    }

    @Test
    fun `getCurrentSyncAccount returns correct account when Dropbox is set`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        assertEquals(SyncAccounts.DROPBOX, accountsRepository.getCurrentSyncAccount())
    }

    @Test
    fun `getCurrentSyncAccount returns correct account when Google Drive is set`() = runTest {
        googleDriveSettings.setGoogleDriveLinked(true)
        assertEquals(SyncAccounts.GOOGLE_DRIVE, accountsRepository.getCurrentSyncAccount())
    }

    @Test
    fun `getCurrentSyncAccount returns correct account when iCloud is set on iOS`() = runTest {
        currentOS = CurrentOS.Ios
        icloudSettings.setUseICloud(true)
        assertEquals(SyncAccounts.ICLOUD, accountsRepository.getCurrentSyncAccount())
    }

    @Test
    fun `getCurrentSyncAccount returns LOCAL when iCloud is set on Android`() = runTest {
        currentOS = CurrentOS.Android
        icloudSettings.setUseICloud(true)
        assertEquals(SyncAccounts.LOCAL, accountsRepository.getCurrentSyncAccount())
    }

    @Test
    fun `isSyncEnabled returns true for Dropbox`() = runTest {
        dropboxSettings.setDropboxData("some-data")
        assertTrue(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns true for Google Drive`() = runTest {
        googleDriveSettings.setGoogleDriveLinked(true)
        assertTrue(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns true for iCloud on iOS`() = runTest {
        currentOS = CurrentOS.Ios
        icloudSettings.setUseICloud(true)
        assertTrue(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns true for iCloud on macOS`() = runTest {
        currentOS = CurrentOS.Desktop.Mac
        icloudSettings.setUseICloud(true)
        assertTrue(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns false for local`() = runTest {
        assertFalse(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns false for FreshRSS`() = runTest {
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncUrl("http://localhost")
        networkSettings.setSyncPwd("pwd")
//        accountsRepository.restoreAccounts()
        assertFalse(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns false for Miniflux`() = runTest {
        networkSettings.setSyncAccountType(SyncAccounts.MINIFLUX)
        networkSettings.setSyncUrl("http://localhost")
        networkSettings.setSyncPwd("pwd")
        assertFalse(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns false for Bazqux`() = runTest {
        networkSettings.setSyncAccountType(SyncAccounts.BAZQUX)
        networkSettings.setSyncUrl("http://localhost")
        networkSettings.setSyncPwd("pwd")
        assertFalse(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns false for Feedbin`() = runTest {
        networkSettings.setSyncAccountType(SyncAccounts.FEEDBIN)
        networkSettings.setSyncPwd("pwd")
        assertFalse(accountsRepository.isSyncEnabled())
    }

    @Test
    fun `isSyncEnabled returns false for iCloud on Android`() = runTest {
        currentOS = CurrentOS.Android
        icloudSettings.setUseICloud(true)
        assertFalse(accountsRepository.isSyncEnabled())
    }
}
