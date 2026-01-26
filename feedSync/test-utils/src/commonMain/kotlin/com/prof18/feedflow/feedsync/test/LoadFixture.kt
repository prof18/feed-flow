package com.prof18.feedflow.feedsync.test

/**
 * Loads a fixture file from the test resources directory.
 *
 * Files should be placed in: feedSync/test-utils/src/commonMain/resources/fixtures/
 *
 * @param filename Path relative to fixtures directory (e.g., "greader/login_success.txt")
 */
expect fun loadFixture(filename: String): String
