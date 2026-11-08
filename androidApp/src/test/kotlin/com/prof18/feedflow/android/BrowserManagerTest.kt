package com.prof18.feedflow.android

import android.app.Application
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.BrowserIds
import com.prof18.feedflow.shared.data.SettingsRepository
import com.russhwolf.settings.MapSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [Build.VERSION_CODES.Q])
class BrowserManagerTest {

    private val application = RuntimeEnvironment.getApplication()
    private val settings = SettingsRepository(MapSettings())

    @Before
    fun installBrowser() {
        registerBrowser(BROWSER)
        settings.saveFavouriteBrowserId(BROWSER)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R, Build.VERSION_CODES.TIRAMISU])
    fun `browser picker lists web handlers and excludes launcher-only apps`() {
        registerBrowser(OTHER_BROWSER)
        registerActivity(
            CALCULATOR,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
        registerActivity(
            INTERNAL_WEB_HANDLER,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("https")
            },
        )
        registerActivity(
            WEB_HANDLER_WITHOUT_DEFAULT_CATEGORY,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addDataScheme("https")
            },
        )

        val browsers = createManager().browserListState.value

        assertEquals(setOf(BrowserIds.IN_APP_BROWSER, BROWSER, OTHER_BROWSER), browsers.map { it.id }.toSet())
        assertEquals(BROWSER, browsers.single { it.isFavourite }.id)
    }

    @Test
    fun `a saved launcher-only favorite falls back to the in-app browser`() {
        registerActivity(
            CALCULATOR,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
        settings.saveFavouriteBrowserId(CALCULATOR)

        val browsers = createManager().browserListState.value

        assertEquals(BrowserIds.IN_APP_BROWSER, browsers.single { it.isFavourite }.id)
    }

    @Test
    fun `explicit browser action goes directly to the selected browser`() {
        registerActivity(
            NATIVE_APP,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("https")
                addDataAuthority("example.com", null)
            },
        )
        val attempts = mutableListOf<Intent>()
        val launchContext = object : ContextWrapper(application) {
            override fun startActivity(intent: Intent) {
                attempts.add(intent)
            }
        }

        val manager = createManager()
        manager.openUrlWithFavoriteBrowser(ARTICLE, launchContext, preferAppHandler = false)

        val intent = attempts.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(ARTICLE, intent.dataString)
        assertEquals(BROWSER, intent.`package`)

        manager.openUrlWithFavoriteBrowser(ARTICLE, launchContext)
        assertEquals(2, attempts.size)
        assertNull(attempts.last().`package`)
    }

    private fun registerActivity(packageName: String, filter: IntentFilter) {
        val component = ComponentName(packageName, "$packageName.MainActivity")
        shadowOf(application.packageManager).apply {
            addActivityIfNotPresent(component)
            addIntentFilterForActivity(component, filter)
        }
    }

    private fun registerBrowser(packageName: String) {
        registerActivity(
            packageName,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("https")
            },
        )
    }

    private fun createManager() = BrowserManager(application, settings, Logger)

    private companion object {
        const val BROWSER = "test.browser"
        const val OTHER_BROWSER = "test.otherbrowser"
        const val CALCULATOR = "test.calculator"
        const val INTERNAL_WEB_HANDLER = "test.internal"
        const val WEB_HANDLER_WITHOUT_DEFAULT_CATEGORY = "test.nocategorydefault"
        const val NATIVE_APP = "test.native"
        const val ARTICLE = "https://example.com/article"
    }
}
