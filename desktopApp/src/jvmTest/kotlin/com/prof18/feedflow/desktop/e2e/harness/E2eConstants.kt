package com.prof18.feedflow.desktop.e2e.harness

import java.io.File

internal const val MAIN_CLASS = "com.prof18.feedflow.desktop.MainKt"
internal const val PREFS_NODE_VALUE = "feedflow-e2e"
internal const val FLOW_TIMEOUT_SECONDS = 180L
internal const val STARTUP_TIMEOUT_MS = 60_000L
internal const val AWAIT_TIMEOUT_MS = 15_000L
internal const val POLL_INTERVAL_MS = 200L
internal const val EVIDENCE_TIMEOUT_MS = 20_000L
internal const val SEED_FAILED_MARKER = "FEEDFLOW_E2E_SEED_FAILED"
internal const val MAIN_READY_TAG = "desktop_main_window_surface"
internal const val HOME_ROOT = "/tmp/ffe2e"
internal const val RUN_LOCK_FILE = "/tmp/ffe2e/run.lock"
internal const val EVIDENCE_DIR = "build/reports/desktop-e2e"
internal const val FIXTURE_FEED_PATH = "/feeds/feedflow-e2e-feed.xml"
internal const val FIXTURE_FEED_TITLE = "FeedFlow E2E Fixture Feed"
internal const val SEED_AUDIO_URL = "https://example.com/episode.mp3?token=seed&part=1"

internal const val SEED_NEWEST_TITLE = "E2E Newest Unread Article: Motorola Razr deals"
internal const val SEED_READ_TITLE = "E2E Read Article: Gemini in Android Auto"
internal const val SEED_BOOKMARKED_UNREAD_TITLE = "E2E Bookmarked Unread Article: Pixel battery debate"
internal const val SEED_BOOKMARKED_READ_TITLE = "E2E Bookmarked Read Article: Quantum computing deal"

// A fresh preferences node opens an 800x600 window, which uses the single-pane layout. Flows expect the
// two-pane layout with the sidebar, so the rule stores a larger window size before every launch.
internal const val WINDOW_WIDTH_DP = 1280
internal const val WINDOW_HEIGHT_DP = 800

internal fun repoRoot(): File =
    File(requireNotNull(System.getProperty("feedflow.e2e.repoRoot")) { "feedflow.e2e.repoRoot is not set" })
