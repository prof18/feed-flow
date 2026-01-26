package com.prof18.feedflow.feedsync.test.di

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.feedbin.di.getFeedbinTestModule
import com.prof18.feedflow.feedsync.greader.di.getGReaderTestModule
import com.prof18.feedflow.feedsync.test.feedbin.FeedbinMockEngineBuilder
import com.prof18.feedflow.feedsync.test.feedbin.configureDefaultFeedbinMocks
import com.prof18.feedflow.feedsync.test.feedbin.createMockFeedbinHttpClient
import com.prof18.feedflow.feedsync.test.greader.GReaderMockEngineBuilder
import com.prof18.feedflow.feedsync.test.greader.configureDefaultGReaderMocks
import com.prof18.feedflow.feedsync.test.greader.createMockGReaderHttpClient
import org.koin.core.module.Module

/**
 * Creates a list of Koin modules that provide mocked GReaderRepository and FeedbinRepository.
 *
 * This function sets up default mock responses for common API endpoints.
 * Import these modules in your test configuration to get mocked repositories
 * automatically injected via DI.
 *
 * Example usage:
 * ```kotlin
 * class MyTest : KoinTestBase() {
 *     override fun getTestModules() = super.getTestModules() + getFeedSyncTestModules()
 *
 *     private val gReaderRepository: GReaderRepository by inject()
 *
 *     @Test
 *     fun testSync() = runTest {
 *         // Use the mocked repository
 *     }
 * }
 * ```
 *
 * For provider-specific mocks (e.g., FreshRSS):
 * ```kotlin
 * getFeedSyncTestModules(
 *     gReaderProvider = SyncAccounts.FRESH_RSS,
 *     gReaderConfig = {
 *         configureFreshRssMocks()
 *     }
 * )
 * ```
 *
 * For custom mock responses:
 * ```kotlin
 * getFeedSyncTestModules(
 *     gReaderConfig = {
 *         addMockResponse(
 *             urlPattern = "/reader/api/0/subscription/list",
 *             responseFile = "my_custom_subscriptions.json"
 *         )
 *     }
 * )
 * ```
 */
fun getFeedSyncTestModules(
    gReaderProvider: SyncAccounts? = null,
    gReaderBaseURL: String = "https://freshrss.example.com/api/greader.php/",
    gReaderConfig: GReaderMockEngineBuilder.() -> Unit = { configureDefaultGReaderMocks() },
    feedbinBaseURL: String = "https://api.feedbin.com/",
    feedbinConfig: FeedbinMockEngineBuilder.() -> Unit = { configureDefaultFeedbinMocks() },
): List<Module> {
    val gReaderHttpClient = createMockGReaderHttpClient(gReaderProvider, gReaderBaseURL, gReaderConfig)
    val feedbinHttpClient = createMockFeedbinHttpClient(feedbinBaseURL, feedbinConfig)

    return listOf(
        getGReaderTestModule(gReaderHttpClient),
        getFeedbinTestModule(feedbinHttpClient),
    )
}
